/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.maven.server.core.reconcile;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.jsonrpc.JsonRpcException;
import org.eclipse.che.api.core.jsonrpc.RequestTransmitter;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.project.server.EditorWorkingCopyManager;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.project.server.notification.EditorContentUpdatedEvent;
import org.eclipse.che.api.project.shared.dto.EditorChangesDto;
import org.eclipse.che.commons.xml.XMLTreeException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.java.shared.dto.Problem;
import org.eclipse.che.ide.ext.java.shared.dto.ReconcileResult;
import org.eclipse.che.ide.maven.tools.Model;
import org.eclipse.che.maven.data.MavenProjectProblem;
import org.eclipse.che.plugin.maven.server.core.MavenProjectManager;
import org.eclipse.che.plugin.maven.server.core.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.eclipse.che.maven.data.MavenConstants.POM_FILE_NAME;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class PomReconciler {
    private static final Logger LOG             = LoggerFactory.getLogger(PomReconciler.class);
    private static final String OUTGOING_METHOD = "event:pom-reconcile-state-changed";

    private ProjectManager                             projectManager;
    private MavenProjectManager                        mavenProjectManager;
    private EditorWorkingCopyManager                   editorWorkingCopyManager;
    private EventService                               eventService;
    private RequestTransmitter                         transmitter;
    private EventSubscriber<EditorContentUpdatedEvent> editorContentUpdateEventSubscriber;

    @Inject
    public PomReconciler(ProjectManager projectManager,
                         MavenProjectManager mavenProjectManager,
                         EditorWorkingCopyManager editorWorkingCopyManager,
                         EventService eventService,
                         RequestTransmitter transmitter) {
        this.projectManager = projectManager;
        this.mavenProjectManager = mavenProjectManager;
        this.editorWorkingCopyManager = editorWorkingCopyManager;
        this.eventService = eventService;
        this.transmitter = transmitter;

        editorContentUpdateEventSubscriber = new EventSubscriber<EditorContentUpdatedEvent>() {
            @Override
            public void onEvent(EditorContentUpdatedEvent event) {
                onEditorContentUpdated(event);
            }
        };
        eventService.subscribe(editorContentUpdateEventSubscriber);
    }

    @PreDestroy
    private void unsubscribe() {
        eventService.unsubscribe(editorContentUpdateEventSubscriber);
    }

    public List<Problem> reconcile(String pomPath)
            throws ServerException, ForbiddenException, NotFoundException, ConflictException {
        VirtualFileEntry entry = projectManager.getProjectsRoot().getChild(pomPath);
        if (entry == null) {
            throw new NotFoundException(format("File '%s' doesn't exist", pomPath));
        }

        String projectPath = entry.getPath().getParent().toString();
        String pomContent = editorWorkingCopyManager.getWorkingCopyContent(pomPath);
        return reconcile(pomPath, projectPath, pomContent);
    }

    private List<Problem> reconcile(String pomPath, String projectPath, String pomContent)
            throws ServerException, NotFoundException {
        List<Problem> result = new ArrayList<>();

        if (isNullOrEmpty(pomContent)) {
            throw new ServerException(format("Couldn't reconcile pom file '%s' because its content is empty", pomPath));
        }

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectPath);
        MavenProject mavenProject = mavenProjectManager.findMavenProject(project);
        if (mavenProject == null) {
            throw new NotFoundException(format("Couldn't reconcile pom file '%s' because maven project is not found", pomPath));
        }

        try {
            Model.readFrom(new ByteArrayInputStream(pomContent.getBytes(defaultCharset())));
            List<MavenProjectProblem> problems = mavenProject.getProblems();

            int start = pomContent.indexOf("<project ") + 1;
            int end = start + "<project ".length();

            List<Problem> problemList = problems.stream().map(mavenProjectProblem -> DtoFactory.newDto(Problem.class)
                                                                                               .withError(true)
                                                                                               .withSourceStart(start)
                                                                                               .withSourceEnd(end)
                                                                                               .withMessage(mavenProjectProblem
                                                                                                                    .getDescription()))
                                                .collect(Collectors.toList());
            result.addAll(problemList);
        } catch (XMLTreeException exception) {
            Throwable cause = exception.getCause();
            if (cause != null && cause instanceof SAXParseException) {
                result.add(createProblem(pomContent, (SAXParseException)cause));

            } else {
                String error = format("Couldn't reconcile pom file '%s', the reason is '%s'", pomPath, exception.getLocalizedMessage());
                LOG.error(error, exception);
                throw new ServerException(error);
            }
        } catch (IOException e) {
            String error = format("Couldn't reconcile pom file '%s', the reason is '%s'", pomPath, e.getLocalizedMessage());
            LOG.error(error, e);
            throw new ServerException(error);
        }
        return result;
    }

    private void onEditorContentUpdated(EditorContentUpdatedEvent event) {
        EditorChangesDto editorChanges = event.getChanges();
        String endpointId = event.getEndpointId();
        String fileLocation = editorChanges.getFileLocation();
        String fileName = new Path(fileLocation).lastSegment();
        if (!POM_FILE_NAME.equals(fileName)) {
            return;
        }

        String projectPath = editorChanges.getProjectPath();
        String newPomContent = event.getNewContent();
        if (isNullOrEmpty(newPomContent)) {
            return;
        }

        try {
            List<Problem> problemList = reconcile(fileLocation, projectPath, newPomContent);
            DtoFactory dtoFactory = DtoFactory.getInstance();
            ReconcileResult reconcileResult = dtoFactory.createDto(ReconcileResult.class)
                                                        .withFileLocation(fileLocation)
                                                        .withProblems(problemList);
            transmitter.transmitOneToNone(endpointId, OUTGOING_METHOD, reconcileResult);
        } catch (Exception e) {
            String error = e.getLocalizedMessage();
            LOG.error(error, e);
            throw new JsonRpcException(500, error, endpointId);
        }
    }

    private Problem createProblem(String pomContent, SAXParseException spe) {
        Problem problem = DtoFactory.newDto(Problem.class);
        problem.setError(true);
        problem.setMessage(spe.getMessage());
        if (pomContent != null) {
            int lineNumber = spe.getLineNumber();
            int columnNumber = spe.getColumnNumber();
            try {
                Document document = new Document(pomContent);
                int lineOffset = document.getLineOffset(lineNumber - 1);
                problem.setSourceStart(lineOffset + columnNumber - 1);
                problem.setSourceEnd(lineOffset + columnNumber);
            } catch (BadLocationException e) {
                LOG.error(e.getMessage(), e);
            }

        }
        return problem;
    }
}
