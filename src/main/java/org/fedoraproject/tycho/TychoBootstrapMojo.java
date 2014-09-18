/*******************************************************************************
 * Copyright (c) 2014 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.tycho;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo( name = "read", aggregator = true, requiresDependencyResolution = ResolutionScope.NONE)
public class TychoBootstrapMojo extends AbstractMojo {

    private static final String EXPECTED_GID = "org.eclipse.tycho";
    private static final String EXPECTED_AID = "tycho";

    /*
     *  TODO : This could be discovered automatically.
     *  Any dependency whose :
     *  - groupId is org.eclipse.tycho
     *  - version is ${tychoBootstrapVersion}
     */
    @Parameter (property = "projects",
            defaultValue = "target-platform-configuration,"
                    + "tycho-maven-plugin,tycho-packaging-plugin,"
                    + "tycho-p2-repository-plugin")
    private String [] targetArtifactIds;

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    private final Map<String, Set<String>> dgraph = new HashMap<String, Set<String>> ();
    private final Map<String, List<Dependency>> idToDeps = new HashMap<String, List<Dependency>> ();

    public void execute() throws MojoExecutionException, MojoFailureException {
        String groupId = session.getCurrentProject().getGroupId();
        String artifactId = session.getCurrentProject().getArtifactId();
        if ( ! (EXPECTED_GID.equals(groupId) && EXPECTED_AID.equals(artifactId))) {
            throw new MojoExecutionException("Mojo must only be run on " + EXPECTED_GID + ":" + EXPECTED_AID);
        }

        getLog().info("Executing Tycho Bootstrap Reader ...");

        createProjectDepMap(idToDeps);

        for (String aid : targetArtifactIds) {
            resolve(aid);
        }

        for (Entry<String, Set<String>> e : dgraph.entrySet()) {
            for (String dep : e.getValue()) {
                System.out.println("\"" + e.getKey() + "\"" + " -> " + "\"" + dep + "\"");
            }
        }

        getLog().info("Completed Tycho Bootstrap Reading!");
    }

    public void resolve (String artifactId) {
        List<Dependency> deps = idToDeps.get(artifactId);
        dgraph.put(artifactId, new HashSet<String> ());
        if (deps != null) {
            for (Dependency dep : deps) {
                if (dep.getGroupId().equals("org.eclipse.tycho")) {
                    String aid = dep.getArtifactId();
                    dgraph.get(artifactId).add(aid);
                    if (dgraph.get(aid) == null) {
                        resolve(aid);
                    }
                }
            }
        }
    }

    public void createProjectDepMap (Map<String, List<Dependency>> mappings) {
        for (MavenProject proj : session.getProjects()) {
            mappings.put(proj.getArtifactId(), proj.getDependencies());
        }
    }

}
