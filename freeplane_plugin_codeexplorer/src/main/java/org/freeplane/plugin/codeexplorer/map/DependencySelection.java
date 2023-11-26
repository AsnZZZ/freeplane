/*
 * Created on 15 Nov 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.map.AncestorRemover;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeRelativePath;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.properties.HasName;

public class DependencySelection {
    private enum Visibility {VISIBLE, HIDDEN_BY_FILTER, HIDDEN_BY_FOLDING, UNKNOWN}

    private final IMapSelection selection;
    private MapModel map;
    private Set<NodeModel> selectedNodeSet;
    private final boolean showsOutsideDependencies;

    public DependencySelection(IMapSelection selection) {
        this(selection, ResourceController.getResourceController().getBooleanProperty("code_showOutsideDependencies", true));
    }

    public DependencySelection(IMapSelection selection, boolean showsOutsideDependencies) {
        this.selection = selection;
        this.showsOutsideDependencies = showsOutsideDependencies;
    }

    public Stream<Dependency> getSelectedDependencies() {
        Set<NodeModel> nodes = AncestorRemover.removeAncestors(getSelectedNodeSet());
        Stream<Dependency> allDependencies = nodes.stream()
                .flatMap(node ->
                Stream.concat(
                        getOutgoingDependencies((CodeNodeModel)node).stream(),
                        getIncomingDependencies((CodeNodeModel)node).stream()))
                .distinct();
        return allDependencies;
    }

    public CodeNodeModel getExistingNode(JavaClass javaClass) {
        String existingNodeId = getExistingNodeId(javaClass);
        return existingNodeId == null ? null : (CodeNodeModel) getMap().getNodeForID(existingNodeId);
    }

    List<JavaClass> getSelectedClasses() {
        List<JavaClass> allClasses;
        Set<NodeModel> nodes = AncestorRemover.removeAncestors(getSelectedNodeSet());
        allClasses = nodes.stream()
                .flatMap(node ->
                Stream.concat(
                        ((CodeNodeModel)node).getOutgoingDependenciesWithKnownTargets().map(Dependency::getOriginClass),
                        ((CodeNodeModel)node).getIncomingDependenciesWithKnownTargets().map(Dependency::getTargetClass)))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        return allClasses;
    }

    public String getVisibleNodeId(JavaClass javaClass) {
        return getExistingNodeId(javaClass, true);
    }

    String getExistingNodeId(JavaClass javaClass) {
        return getExistingNodeId(javaClass, false);
    }

    private String getExistingNodeId(JavaClass javaClass, boolean visibleOnly) {
        JavaClass targetClass = CodeNodeModel.findEnclosingNamedClass(javaClass);
        String targetClassId = targetClass.getName();
        if(! visibleOnly && getNode(targetClassId) != null)
            return targetClassId;
        if(visibleOnly) {
            switch(visibility(targetClassId)) {
            case VISIBLE:
                return targetClassId;
            case HIDDEN_BY_FILTER:
                return null;
            default:
                break;
            }
        }
        JavaPackage targetPackage = targetClass.getPackage();
        String targetPackageClassesId = targetPackage.getName() + ".package";
        if(! visibleOnly && getNode(targetPackageClassesId) != null)
            return targetPackageClassesId;
        if(visibleOnly) {
            switch(visibility(targetPackageClassesId)) {
            case VISIBLE:
                return targetPackageClassesId;
            case HIDDEN_BY_FILTER:
                return null;
            default:
                break;
            }
        }
        HasName visiblePackage = getContainingPackage(targetPackage, visibleOnly);
        return visiblePackage == null ? null : visiblePackage.getName();
    }

     private Set<Dependency> getOutgoingDependencies(CodeNodeModel node) {
         Stream<Dependency> dependencies = node.getOutgoingDependenciesWithKnownTargets();
         return dependenciesBetweenDifferentElements(dependencies);
     }
     private Set<Dependency> getIncomingDependencies(CodeNodeModel node) {
         Stream<Dependency> dependencies = node.getIncomingDependenciesWithKnownTargets();
         return dependenciesBetweenDifferentElements(dependencies);
     }

     private Set<Dependency> dependenciesBetweenDifferentElements(Stream<Dependency> dependencies) {
         Set<Dependency> filteredDependencies = dependencies
                 .filter(dependency -> connectsDifferentElements(dependency))
                 .collect(Collectors.toSet());
         return filteredDependencies;
     }
     private NodeModel getNode(String id) {
         return getMap().getNodeForID(id);
    }

    private NodeModel findSelectedAncestorOrSelf(NodeModel node) {
        while(node != null && ! selectionContains(node))
             node = node.getParentNode();
         return node;
    }

    NodeModel findVisibleAncestorOrSelf(NodeModel node) {
        while(node != null && ! selection.isVisible(node))
             node = node.getParentNode();
         return node;
    }

    private boolean selectionContains(NodeModel node) {
        return getSelectedNodeSet().contains(node);
    }

    private Set<NodeModel> getSelectedNodeSet() {
        if(selectedNodeSet == null)
            selectedNodeSet = selection.getSelection();
        return selectedNodeSet;
    }

    private MapModel getMap() {
        if(map == null)
            map = selection.getMap();
        return map;
    }


    private HasName getContainingPackage(JavaPackage targetPackage, boolean visibleOnly) {
         for(;;) {
             String targetPackageName = targetPackage.getName();
             NodeModel targetNode = getNode(targetPackageName);
             if(targetNode != null) {
                 if(! visibleOnly || selection.isVisible(targetNode))
                     return targetPackage;
                 if(! targetNode.isVisible(selection.getFilter()))
                     return null;
             }
             Optional<JavaPackage> parent = targetPackage.getParent();
             if(! parent.isPresent())
                 return null;
             targetPackage = parent.get();
         }
     }

    private Visibility visibility(String targetId) {
        NodeModel targetNode = getNode(targetId);
        if(targetNode == null)
            return Visibility.UNKNOWN;
        if(selection.isVisible(targetNode))
            return Visibility.VISIBLE;
        if(! targetNode.isVisible(selection.getFilter()))
            return Visibility.HIDDEN_BY_FILTER;
        return Visibility.HIDDEN_BY_FOLDING;
    }

    private boolean connectsDifferentElements(Dependency dependency) {
        String visibleOriginId = getVisibleNodeId(dependency.getOriginClass());
         String visibleTargetId = getVisibleNodeId(dependency.getTargetClass());
         if (visibleOriginId == null  || visibleTargetId == null || visibleOriginId.equals(visibleTargetId))
             return false;
         if (getSelectedNodeSet().size() == 1 || this.showsOutsideDependencies)
             return true;
         NodeModel visibleOrigin = getNode(visibleOriginId);
         NodeModel selectedVisibleOrigin = findSelectedAncestorOrSelf(visibleOrigin);
         if(selectedVisibleOrigin ==  null)
             return false;
         NodeModel visibleTarget = getNode(visibleTargetId);
         NodeModel selectedVisibleTarget = findSelectedAncestorOrSelf(visibleTarget);
         return selectedVisibleTarget != null && selectedVisibleTarget != selectedVisibleOrigin
                 && (! visibleTarget.isDescendantOf(selectedVisibleOrigin) || ! visibleOrigin.isDescendantOf(selectedVisibleTarget));
     }

     public boolean isConnectorSelected(CodeNodeModel source, CodeNodeModel target) {
         Set<NodeModel> selectedNodes = getSelectedNodeSet();
         boolean isOnlyOneNodeSelected = selectedNodes.size() == 1;
         if(isOnlyOneNodeSelected && selection.getSelected().isRoot())
             return false;
         NodeModel selectedSourceAncestorOrSource = findSelectedAncestorOrSelf(source);
         boolean isSourceSelected = selectedSourceAncestorOrSource != null;
         boolean showsOutsideDependencies = isOnlyOneNodeSelected || this.showsOutsideDependencies;
         if (showsOutsideDependencies && isSourceSelected)
             return ! target.isDescendantOf(selectedSourceAncestorOrSource);
         NodeModel selectedTargetAncestorOrTarget = findSelectedAncestorOrSelf(target);
         boolean isTargetSelected = selectedTargetAncestorOrTarget != null;
         if (showsOutsideDependencies)
             return isTargetSelected && ! source.isDescendantOf(selectedTargetAncestorOrTarget);
         else if (isSourceSelected && isTargetSelected)
             return selectedSourceAncestorOrSource != selectedTargetAncestorOrTarget;
         else
             return false;
     }
 }