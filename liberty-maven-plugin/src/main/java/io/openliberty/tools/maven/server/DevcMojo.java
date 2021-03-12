/**
 * (C) Copyright IBM Corporation 2020.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.maven.server;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/* graphBuilder */

import org.apache.maven.plugins.annotations.Component;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Exclusion;

import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyNode;
//org/apache/maven/plugins/dependency/tree/VerboseDependencyGraphBuilder.class
//import org.apache.maven.plugins.dependency.tree.VerboseDependencyGraphBuilder;
import org.apache.maven.plugins.dependency.tree.*;
//import org.apache.maven.shared.dependency.*;
//import org.apache.maven.plugins.*;
//import org.apache.maven.shared.dependency.graph.VerboseDependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.FilteringDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.apache.maven.shared.dependency.graph.filter.ArtifactDependencyNodeFilter;

/* Aether */

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

/**
 * Start dev mode for containers
 */
@Mojo(name = "devc", requiresDependencyCollection = ResolutionScope.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class DevcMojo extends DevMojo {
    /*
     * @Parameter(defaultValue = "${project}", readonly = true, required = true)
     * MavenProject project;
     */

    /*
     * @Parameter(defaultValue = "${session}", readonly = true, required = true)
     * private MavenSession session;
     */

    @Parameter(property = "aether", defaultValue = "false")
    private boolean aether;

    @Parameter(property = "filter", defaultValue = "false")
    private boolean filter;

    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose;

    @Parameter(property = "test", defaultValue = "false")
    private boolean test;

    @Parameter(property = "includes")
    private String includes;

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Component
    ProjectDependenciesResolver resolver;

    @Override
    protected void doExecute() throws Exception {
        if (test) {
            testMavenStuff();
        } else if (aether) {
            if (filter) {
                doAetherFilterMethod();
            }
            else {
                doAetherMethod();
            }
        } else if (verbose) {
            if (filter) {
                doVerboseFilterMethod();
            }
            else {
                doVerboseMethod();
            }
        } else {
            if (filter) {
                doFilterMethod();
            }
            else {
                doGraphBuilderMethod();
            }
        }
    }

    private void doGraphBuilderMethod() throws DependencyGraphBuilderException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);
        DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null, reactorProjects);
        log.debug("***** Root Node *****");
        log.debug(rootNode.toNodeString());
        log.debug(rootNode.toString());
        log.debug("***** Base dependency graph *****");
        printChildren(rootNode, 0);
    }

    private void doFilterMethod() throws DependencyGraphBuilderException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);
        DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null, reactorProjects);
        //CollectingDependencyNodeVisitor visitor = new CollectingDependencyNodeVisitor();
        // rootNode.accept(visitor);
        /*
         * List<DependencyNode> nodes = visitor.getNodes(); for (DependencyNode
         * dependencyNode : nodes) { dependencyNode. }
         */

        /*
         * ***Filtering***
         */
        CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
        DependencyNodeFilter depFilter = createDependencyNodeFilter(includes);
        DependencyNodeVisitor firstPassVisitor = new FilteringDependencyNodeVisitor( collectingVisitor, depFilter );
        rootNode.accept( firstPassVisitor );
        DependencyNodeFilter secondPassFilter = new AncestorOrSelfDependencyNodeFilter( collectingVisitor.getNodes() );
        CollectingDependencyNodeVisitor collectingVisitor2 = new CollectingDependencyNodeVisitor();
        DependencyNodeVisitor buildingVisitor = new BuildingDependencyNodeVisitor(collectingVisitor2);
        buildingVisitor = new FilteringDependencyNodeVisitor( buildingVisitor, secondPassFilter );
        rootNode.accept( buildingVisitor );
        List<DependencyNode> nodes = collectingVisitor2.getNodes();
        log.debug("Node length: " + nodes.size());
        for (DependencyNode dependencyNode : nodes) {
            log.debug(dependencyNode.getArtifact().toString());
        }
        //try printchildren on first node??
        //***********************************************/
    }

    private void doVerboseMethod() throws DependencyGraphBuilderException {
        log.debug("***** Create ProjectBuildingRequest *****");
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        log.debug("***** Set Project *****");
        buildingRequest.setProject(project);

        // verboseGraphBuilder needs MavenProject project, RepositorySystemSession session,
        // ProjectDependenciesResolver resolver
        log.debug("***** Create new VerboseDependencyGraphBuilder *****");
        VerboseDependencyGraphBuilder builder = new VerboseDependencyGraphBuilder();
        //AbstractVerboseGraphSerializer serializer = getSerializer();
        log.debug("***** Build verbose graph *****");
        org.eclipse.aether.graph.DependencyNode verboseRootNode = builder.buildVerboseGraph(
                project, resolver, repoSession, reactorProjects, buildingRequest );
        log.debug("***** Verbose dependency graph 1 *****");
        printChildren(verboseRootNode, 0);
        //dependencyTreeString = serializer.serialize( verboseRootNode );
        log.debug("***** Convert from aether to Maven *****");
        DependencyNode rootNode = convertToCustomDependencyNode( verboseRootNode );
        log.debug("***** Verbose dependency graph 2 *****");
        printChildren(rootNode, 0);
    }

    private void doVerboseFilterMethod() throws DependencyGraphBuilderException {
    }

    private void doAetherMethod() throws MojoExecutionException, DependencyResolutionException {
        List<Dependency> dependencies = project.getDependencies();
        List<Artifact> artifacts = new ArrayList<Artifact>();
        for (Dependency dep : dependencies) {
            Artifact artifact = getArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getVersion());
            artifacts.add(artifact);
        }
        for (Artifact artifact : artifacts) {
            org.eclipse.aether.artifact.Artifact aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
                    artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getVersion());
            org.eclipse.aether.graph.Dependency dependency = new org.eclipse.aether.graph.Dependency(aetherArtifact, null, true);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.setRepositories(repositories);
            log.debug("collectRequest:");
            log.debug(collectRequest.toString());
            
            DependencyRequest depRequest = new DependencyRequest(collectRequest, null);
            log.debug("depRequest:");
            log.debug(depRequest.toString());

            DependencyResult dependencyResult = repositorySystem.resolveDependencies(repoSession, depRequest);
            log.debug("dependencyResult:");
            log.debug(dependencyResult.toString());
            org.eclipse.aether.graph.DependencyNode rootNode = dependencyResult.getRoot();
            log.debug("root:");
            log.debug(rootNode.toString());
            printChildren(rootNode, 0);
        }
    }

    private void testMavenStuff() throws MojoExecutionException, DependencyResolutionException {
        List<Dependency> dependencies = project.getDependencies();
        log.debug("Dependencies");
        for (Dependency d : dependencies) {
            log.debug(d.getArtifactId());
        }
        Set<Artifact> artifacts = project.getArtifacts();
        log.debug("Artifacts");
        for (Artifact a : artifacts) {
            log.debug(a.getArtifactId());
        }
    }

    private void doAetherFilterMethod() throws MojoExecutionException, DependencyResolutionException {
        log.debug("<<<<<<<<<<<< doAetherFilterMethod >>>>>>>>>>>");
        List<Dependency> dependencies = project.getDependencies();
        List<Artifact> artifacts = new ArrayList<Artifact>();
        for (Dependency dep : dependencies) {
            Artifact artifact = getArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getVersion());
            artifacts.add(artifact);
        }

        List<List<org.eclipse.aether.graph.DependencyNode>> allPaths = new ArrayList<List<org.eclipse.aether.graph.DependencyNode>>();

        for (Artifact artifact : artifacts) {
            org.eclipse.aether.artifact.Artifact aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
                    artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getVersion());
            org.eclipse.aether.graph.Dependency dependency = new org.eclipse.aether.graph.Dependency(aetherArtifact, null, true);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.setRepositories(repositories);
            //log.debug("collectRequest:");
            //log.debug(collectRequest.toString());
            
            DependencyRequest depRequest = new DependencyRequest(collectRequest, null);
            //log.debug("depRequest:");
            //log.debug(depRequest.toString());

            DependencyResult dependencyResult = repositorySystem.resolveDependencies(repoSession, depRequest);
            //log.debug("dependencyResult:");
            //log.debug(dependencyResult.toString());
            org.eclipse.aether.graph.DependencyNode rootNode = dependencyResult.getRoot();
            //log.debug("Unfiltered root:");
            //log.debug(rootNode.toString());
            //log.debug("Unfiltered tree:");
            //printChildren(rootNode, 0);
            
            //PathRecordingDependencyVisitor - WORKS
            org.eclipse.aether.graph.DependencyFilter depFilter = new org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter(includes);
            org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor filteringVisitor =  new org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor(depFilter);
            rootNode.accept(filteringVisitor);
            List<List<org.eclipse.aether.graph.DependencyNode>> nodeList = filteringVisitor.getPaths();
            if (nodeList == null || nodeList.isEmpty()) {
                log.debug("No Paths");
            }
            else {
                for (List<org.eclipse.aether.graph.DependencyNode> pathList : nodeList) {
                    allPaths.add(pathList);
                    log.debug("Path added");
                }
                //int i = 0;
                /*for (List<org.eclipse.aether.graph.DependencyNode> pathList : nodeList) {
                    log.debug("----------------------------------------------------------");
                    log.debug("<<< Path " + ++i + " >>>");
                    for (org.eclipse.aether.graph.DependencyNode node : pathList) {
                        log.debug(node.getArtifact().toString());
                    }
                    log.debug("----------------------------------------------------------");
                }*/
            }

            /* FilteringDependencyVisitor
            org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor visitor = new org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor();
            org.eclipse.aether.graph.DependencyFilter depFilter = new org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter(includes);
            org.eclipse.aether.graph.DependencyVisitor filteringVisitor =  new org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor(visitor, depFilter);
            
            rootNode.accept(filteringVisitor);
            org.eclipse.aether.graph.DependencyNode filteredRootNode = visitor.getRootNode();
            
            /*if (filteredRootNode != null) {
                log.debug("Filtered root:");
                log.debug(filteredRootNode.toString());
                log.debug("Filtered tree:");
                printChildren(filteredRootNode, 0);
            }
            else {
                log.debug("Nothing left after filter.");
            }*/
        }

        int i = 0;
        for (List<org.eclipse.aether.graph.DependencyNode> pathList : allPaths) {
            log.debug("----------------------------------------------------------");
            log.debug("<<< Path " + ++i + " >>>");
            for (org.eclipse.aether.graph.DependencyNode node : pathList) {
                log.debug(node.getArtifact().toString());
            }
            log.debug("----------------------------------------------------------");
        }
    }

    private void printChildren(DependencyNode rootNode, int level) {
        log.debug(level + ": " + rootNode.toNodeString());
        if (rootNode.getChildren() != null && !rootNode.getChildren().isEmpty()) {
            for (DependencyNode node : rootNode.getChildren()) {
                printChildren(node, level + 1);
            }
        }
    }

    private void printChildren(org.eclipse.aether.graph.DependencyNode rootNode, int level) {
        log.debug(level + ": " + rootNode.getArtifact().toString());
        if (rootNode.getChildren() != null && !rootNode.getChildren().isEmpty()) {
            for (org.eclipse.aether.graph.DependencyNode node : rootNode.getChildren()) {
                printChildren(node, level + 1);
            }
        }
    }

    private DependencyNodeFilter createDependencyNodeFilter(String includes)
    {
        //List<DependencyNodeFilter> filters = new ArrayList<>();

        // filter includes
        if ( includes != null )
        {
            List<String> patterns = Arrays.asList( includes.split( "," ) );

            getLog().debug( "+ Filtering dependency tree by artifact include patterns: " + patterns );

            ArtifactFilter artifactFilter = new StrictPatternIncludesArtifactFilter( patterns );
            return new ArtifactDependencyNodeFilter( artifactFilter );
            //filters.add( new ArtifactDependencyNodeFilter( artifactFilter ) );
        }
        
        return null;

        // filter excludes
        /*if ( excludes != null )
        {
            List<String> patterns = Arrays.asList( excludes.split( "," ) );

            getLog().debug( "+ Filtering dependency tree by artifact exclude patterns: " + patterns );

            ArtifactFilter artifactFilter = new StrictPatternExcludesArtifactFilter( patterns );
            filters.add( new ArtifactDependencyNodeFilter( artifactFilter ) );
        }*/

        //return filters.isEmpty() ? null : new  ( filters );
    }

    private DependencyNode convertToCustomDependencyNode( org.eclipse.aether.graph.DependencyNode node )
    {
        log.debug("***** Beginning convertToCustomDependencyNode *****");
        DefaultDependencyNode rootNode = new DefaultDependencyNode( null,
                convertAetherArtifactToMavenArtifact( node ), null, null, null );

        rootNode.setChildren( new ArrayList<DependencyNode>() );

        for ( org.eclipse.aether.graph.DependencyNode child : node.getChildren() )
        {
            rootNode.getChildren().add( buildTree( rootNode, child ) );
        }

        return rootNode;
    }

    private DependencyNode buildTree( DependencyNode parent, org.eclipse.aether.graph.DependencyNode child )
    {
        log.debug("***** Beginning buildTree *****");
        List<org.apache.maven.model.Exclusion> exclusions = new ArrayList<>();

        for ( org.eclipse.aether.graph.Exclusion exclusion : child.getDependency().getExclusions() )
        {
            exclusions.add( convertAetherExclusionToMavenExclusion( exclusion ) );
        }

        DefaultDependencyNode newChild = new DefaultDependencyNode( parent,
                convertAetherArtifactToMavenArtifact( child ),
                child.getArtifact().getProperties().get( "preManagedVersion" ),
                child.getArtifact().getProperties().get( "preManagedScope" ), null,
                child.getDependency().isOptional() );

        newChild.setChildren( new ArrayList<DependencyNode>() );

        for ( org.eclipse.aether.graph.DependencyNode grandChild : child.getChildren() )
        {
            newChild.getChildren().add( buildTree( newChild, grandChild ) );
        }

        return newChild;
    }

    private static Artifact convertAetherArtifactToMavenArtifact( org.eclipse.aether.graph.DependencyNode node )
    {
        System.out.println("***** Beginning convertAetherArtifactToMavenArtifact *****");
        org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
        return new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getVersion(), node.getDependency().getScope(), artifact.getExtension(),
                artifact.getClassifier(), null );
    }

    private static Exclusion convertAetherExclusionToMavenExclusion ( org.eclipse.aether.graph.Exclusion exclusion )
    {
        System.out.println("***** Beginning convertAetherExclusionToMavenExclusion *****");
        Exclusion mavenExclusion = new Exclusion();
        mavenExclusion.setArtifactId( exclusion.getArtifactId() );
        mavenExclusion.setGroupId( exclusion.getGroupId() );
        // don't do anything with locations yet
        return  mavenExclusion;
    }

}