package org.freeplane.plugin.codeexplorer.configurator;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.plugin.codeexplorer.task.CodeExplorerConfiguration;

class CodeExplorerConfigurator extends JPanel {

    private static final String CODE_EXPLORER_CONFIGURATION_PROPERTY = "code_explorer_configuration";
    private static final long serialVersionUID = 1L;
    private DefaultTableModel configTableModel;
    private DefaultTableModel locationsTableModel;
    private JTable configTable;
    private JTable locationsTable;
    private final CodeExplorerConfigurations explorerConfigurations;
    private final CodeProjectController codeProjectController;

    CodeExplorerConfigurator(CodeProjectController codeProjectController) {
        this.codeProjectController = codeProjectController;
        this.explorerConfigurations = loadConfigurations();
        initializeComponents();
        updateConfigurationsTable(explorerConfigurations);
    }

    private CodeExplorerConfigurations loadConfigurations() {
        String codeExplorerConfiguration = ResourceController.getResourceController().getProperty(CODE_EXPLORER_CONFIGURATION_PROPERTY, "");
        CodeExplorerConfigurations explorerConfigurations = CodeExplorerConfigurations.deserialize(codeExplorerConfiguration);
        return explorerConfigurations;
    }

    private void saveConfigurationsProperty() {
        String spec = explorerConfigurations.serialize();
        ResourceController.getResourceController().setProperty(CODE_EXPLORER_CONFIGURATION_PROPERTY, spec);
    }

    private void updateConfigurationsTable(CodeExplorerConfigurations explorerConfigurations) {
        configTableModel.setRowCount(0); // Clear existing data
        for (CodeExplorerConfiguration config : explorerConfigurations.getConfigurations()) {
            configTableModel.addRow(new Object[]{config.getProjectName()});
        }
    }


    private void initializeComponents() {
        createConfigurationsPanel();
        createLocationsPanel();
        layoutPanels();
    }

    private JPanel createConfigurationsPanel() {
        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createGridBagConstraints();

        configTableModel = new DefaultTableModel(new Object[]{"Configurations"}, 0);
        configTable = new JTable(configTableModel);
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane configTableScrollPane = new JScrollPane(configTable);
        addComponentToPanel(configTableScrollPane, configPanel, gbc, 0, 0, 1, 1);

        configTable.getSelectionModel().addListSelectionListener(e -> updateLocationsTable());

        configTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int firstRow = e.getFirstRow();
                if (e.getType() == TableModelEvent.UPDATE && firstRow >= 0) {
                    int lastRow = e.getLastRow();
                    updateConfigurationNames(firstRow, lastRow);
                }
            }
        });

        JPanel configButtonsPanel = createConfigButtonsPanel();
        addComponentToPanel(configButtonsPanel, configPanel, gbc, 0, 1, 1, 0);
        return configPanel;
    }


    private void updateConfigurationNames(int firstRow, int lastRow) {
        for (int row = firstRow; row < lastRow; row++) {
            updateConfigurationName(row);
        }
        saveConfigurationsProperty();
    }

    private void updateConfigurationName(int row) {
        String projectName = ((String) configTableModel.getValueAt(row, 0)).trim();
        CodeExplorerConfiguration config = getConfiguration(row);
        config.setProjectName(projectName);
    }

    private void updateLocationsTable() {
        locationsTableModel.setRowCount(0); // Clear existing data
        int selectedRow = getSelectedConfigurationIndex();
        if (selectedRow >= 0) {
            CodeExplorerConfiguration config = getConfiguration(selectedRow);
            for (File location : config.getLocations()) {
                locationsTableModel.addRow(new Object[]{location.getAbsolutePath()});
            }
        }
    }

    private JPanel createConfigButtonsPanel() {
        JPanel configButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addConfigurationButton = TranslatedElementFactory.createButton("code.add");
        addConfigurationButton.addActionListener(e -> addNewConfiguration());
        JButton deleteConfigurationButton = TranslatedElementFactory.createButton("code.delete");
        deleteConfigurationButton.addActionListener(e -> deleteSelectedConfiguration());
        JButton exploreConfigurationButton = TranslatedElementFactory.createButton("code.explore");
        exploreConfigurationButton.addActionListener(e -> codeProjectController.exploreConfiguration(getSelectedConfiguration()));
        configButtonsPanel.add(addConfigurationButton);
        configButtonsPanel.add(deleteConfigurationButton);
        configButtonsPanel.add(exploreConfigurationButton);
        return configButtonsPanel;
    }

    CodeExplorerConfiguration getSelectedConfiguration() {
        int selectedConfigurationIndex = getSelectedConfigurationIndex();
        CodeExplorerConfiguration selectedConfiguration = getConfiguration(selectedConfigurationIndex);
        return selectedConfiguration;
    }

    private CodeExplorerConfiguration getConfiguration(int selectedConfigurationIndex) {
        if(selectedConfigurationIndex >= 0)
            return explorerConfigurations.getConfigurations().get(selectedConfigurationIndex);
        else
            return null;
    }

    private int getSelectedConfigurationIndex() {
        return configTable.getSelectedRow();
    }

    private void addNewConfiguration() {
        CodeExplorerConfiguration newConfig = new CodeExplorerConfiguration("", new ArrayList<>());
        explorerConfigurations.getConfigurations().add(newConfig);
        configTableModel.addRow(new Object[]{newConfig.getProjectName()});
        int newRow = configTable.getRowCount() - 1;
        configTable.setRowSelectionInterval(newRow, newRow);
        configTable.editCellAt(newRow, 0);
        configTable.getEditorComponent().requestFocusInWindow();
    }

    private void deleteSelectedConfiguration() {
        int selectedRow = getSelectedConfigurationIndex();
        if (selectedRow >= 0) {
            configTableModel.removeRow(selectedRow);
            explorerConfigurations.getConfigurations().remove(selectedRow);
            saveConfigurationsProperty();
            int rowCount = configTableModel.getRowCount();
            if(selectedRow < rowCount)
                configTable.setRowSelectionInterval(selectedRow, selectedRow);
            else if (rowCount > 0)
                configTable.setRowSelectionInterval(rowCount-1, rowCount-1);
            updateLocationsTable();
        }
    }

    private JPanel createLocationsPanel() {
        JPanel locationsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createGridBagConstraints();

        locationsTableModel = new DefaultTableModel(new Object[]{"Locations"}, 0);
        locationsTable = new JTable(locationsTableModel);
        locationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane locationsTableScrollPane = new JScrollPane(locationsTable);
        addComponentToPanel(locationsTableScrollPane, locationsPanel, gbc, 0, 0, 1, 1);

        JPanel locationsButtonsPanel = createLocationsButtonsPanel();
        addComponentToPanel(locationsButtonsPanel, locationsPanel, gbc, 0, 1, 1, 0);
        return locationsPanel;
    }

    private JPanel createLocationsButtonsPanel() {
        JPanel locationsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addJarsButton = TranslatedElementFactory.createButton("code.add_location");
        addJarsButton.addActionListener(e -> addJarsAndFolders());
        JButton removeLocationsButton = TranslatedElementFactory.createButton("code.remove_location");
        removeLocationsButton.addActionListener(e -> deleteSelectedLocation());
        locationsButtonsPanel.add(addJarsButton);
        locationsButtonsPanel.add(removeLocationsButton);
        addJarsButton.setEnabled(false);
        removeLocationsButton.setEnabled(false);

        configTable.getSelectionModel().addListSelectionListener(l -> {
            boolean isSelectionValid = ((ListSelectionModel)l.getSource()).getMinSelectionIndex() >= 0;
            addJarsButton.setEnabled(isSelectionValid);
            removeLocationsButton.setEnabled(isSelectionValid);
        });
        return locationsButtonsPanel;
    }

    private void addJarsAndFolders() {
        if(configTable.getRowCount() == 0)
            addNewConfiguration();
        JFileChooser fileChooser = UITools.newFileChooser(null);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("jar", "jar");
        fileChooser.setFileFilter(filter);
        int option = fileChooser.showOpenDialog(CodeExplorerConfigurator.this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            int selectedConfigRow = getSelectedConfigurationIndex();
            if (selectedConfigRow >= 0) {
                CodeExplorerConfiguration selectedConfig = getConfiguration(selectedConfigRow);
                for (File file : files) {
                    locationsTableModel.addRow(new Object[]{file.getAbsolutePath()});
                    selectedConfig.getLocations().add(file);
                }
            }
            saveConfigurationsProperty();
        }
    }

    private void deleteSelectedLocation() {
        int selectedIndex = locationsTable.getSelectedRow();
        if (selectedIndex != -1) {
            locationsTableModel.removeRow(selectedIndex);
            int selectedConfigRow = getSelectedConfigurationIndex();
            if (selectedConfigRow >= 0) {
                CodeExplorerConfiguration config = getConfiguration(selectedConfigRow);
                config.getLocations().remove(selectedIndex);
            }
            saveConfigurationsProperty();
        }
    }
    private void layoutPanels() {
        setLayout(new GridLayout(1, 2));
        add(createConfigurationsPanel());
        add(createLocationsPanel());
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        return gbc;
    }

    private void addComponentToPanel(Component component, JPanel panel, GridBagConstraints gbc,
                                     int gridx, int gridy, double weightx, double weighty) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        panel.add(component, gbc);
    }

    public List<File> getSelectedLocations() {
        List<File> paths = new ArrayList<>();
        for (int i = 0; i < locationsTableModel.getRowCount(); i++) {
            paths.add(new File(locationsTableModel.getValueAt(i, 0).toString()));
        }
        return paths;
    }
}
