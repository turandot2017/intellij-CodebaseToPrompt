package com.github.codebase2prompt.action;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.jetbrains.annotations.NotNull;

import com.github.codebase2prompt.ui.PromptGeneratorDialog;
import com.intellij.find.impl.FindPopupPanel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.messages.MessageBusConnection;

public class FindInFilesListener implements ToolWindowManagerListener {
    private static final Logger LOG = Logger.getInstance(FindInFilesListener.class);

    private AWTEventListener awtEventListener;
    private final Project project;



    public FindInFilesListener(Project project) {
        this.project = project;
    }

    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        ToolWindow toolWindow = toolWindowManager.getToolWindow("Find");
        if (toolWindow != null) {
            LOG.debug("Find toolWindow created");
            // 只要窗口存在就添加监听器
            setupWindowListener(toolWindow);
        }
    }


    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if ("Find".equals(toolWindow.getId())) {
            LOG.debug("Find window shown");
            processToolWindow(toolWindow);
        }
    }

    private void setupWindowListener(ToolWindow toolWindow) {
        if (awtEventListener == null) {
            awtEventListener = event -> {
                if (event.getID() == ContainerEvent.COMPONENT_ADDED) {
                    ContainerEvent containerEvent = (ContainerEvent) event;
                    Component child = containerEvent.getChild();
                    if (child instanceof FindPopupPanel) {
                        LOG.info("FindPopupPanel component detected, adding Codebase2Prompt button");
                        addButtonToFindPopupPanel((FindPopupPanel) child);
                    }
                }
            };
            Toolkit.getDefaultToolkit()
                .addAWTEventListener(awtEventListener, AWTEvent.CONTAINER_EVENT_MASK);
            LOG.debug("AWT event listener setup completed");
        }
    }

    private void processToolWindow(ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        Content[] contents = contentManager.getContents();

        SwingUtilities.invokeLater(() -> {
            for (Content content : contents) {
                JComponent component = content.getComponent();
                findAndAddButton(component);
            }
        });
    }

    // 添加递归搜索方法
    private void findAndAddButton(Container container) {

        Component[] components = container.getComponents();
        for (Component component : components) {
            if (component instanceof FindPopupPanel) {
                LOG.info("Found FindPopupPanel, adding Codebase2Prompt button");
                addButtonToFindPopupPanel((FindPopupPanel) component);
                return;
            }
            if (component instanceof Container) {
                findAndAddButton((Container) component);
            }
        }
    }

    private void addButtonToFindPopupPanel(FindPopupPanel findPopupPanel) {
        // 确保在 EDT 线程中执行 UI 操作
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addButtonToFindPopupPanel(findPopupPanel));
            return;
        }

        JButton myButton = new JButton("Codebase2Prompt");
        myButton.addActionListener(event -> {
            LOG.info("Codebase2Prompt button clicked");

            // 获取 PsiFile 列表
            Set<PsiFile> psiFileSet = getPsiFileList(findPopupPanel);
            if (psiFileSet != null) {
                // 将 Set 转换为数组
                PsiFile[] psiFiles = psiFileSet.toArray(new PsiFile[0]);
                // 创建并显示对话框
                PromptGeneratorDialog dialog = new PromptGeneratorDialog(project, psiFiles);
                dialog.show();
            } else {
                LOG.warn("Could not retrieve file list.");
            }
        });

        // 使用更可靠的方式添加按钮
        Container parent = findPopupPanel.getParent();
        if (parent != null && parent.getLayout() instanceof BorderLayout) {
            parent.add(myButton, BorderLayout.SOUTH);
            parent.revalidate();
            parent.repaint();
            LOG.info("Added Codebase2Prompt button to FindPopupPanel.");
        } else {
            LOG.warn("Parent layout is not BorderLayout, cannot add button.");
        }

    }

    private Set<PsiFile> getPsiFileList(FindPopupPanel findPopupPanel) {
        try {
            // 1. 先获取 myResultsPreviewTable (兼容 2020.1)
            Field tableField = findFieldInHierarchy(findPopupPanel.getClass(), "myResultsPreviewTable");
            if (tableField == null) {
                LOG.error("Could not find myResultsPreviewTable field in FindPopupPanel or its superclasses");
                return null;
            }
            tableField.setAccessible(true);
            JComponent table = (JComponent) tableField.get(findPopupPanel);

            // 2. 获取 table 的 model
            Field modelField = findFieldInHierarchy(table.getClass(), "dataModel");
            if (modelField == null) {
                LOG.error("Could not find dataModel field in table");
                return null;
            }
            modelField.setAccessible(true);
            DefaultTableModel model = (DefaultTableModel) modelField.get(table);

            // 2. 遍历 model 中的数据并使用 HashSet 去重
            Set<PsiFile> psiFileSet = new HashSet<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object value = model.getValueAt(i, 0);
                Object valueItem = value;

                // 使用反射检查是否存在 FindPopupItem 类(兼容 2024.1)
                try {
                    Class<?> findPopupItemClass = Class.forName("com.intellij.find.impl.FindPopupItem");
                    if (findPopupItemClass.isInstance(value)) {
                        // 如果是 FindPopupItem 类型，通过反射获取 usage
                        Method getUsageMethod = findPopupItemClass.getMethod("getUsage");
                        valueItem = getUsageMethod.invoke(value);
                    }
                } catch (ClassNotFoundException e) {
                    // FindPopupItem 类不存在，说明是旧版本 IDEA，直接使用原值
                    valueItem = value;
                } catch (Exception e) {
                    LOG.warn("Error handling FindPopupItem", e);
                    valueItem = value;
                }

                if (valueItem instanceof UsageInfo2UsageAdapter) {
                    UsageInfo2UsageAdapter usageAdapter = (UsageInfo2UsageAdapter) valueItem;
                    UsageInfo usageInfo = usageAdapter.getUsageInfo();
                    PsiFile psiFile = usageInfo.getFile();
                    if (psiFile != null) {
                        psiFileSet.add(psiFile);
                    }
                } else {
                    if (i == 0) {
                        LOG.warn("Model row class not defined: " + value.getClass().getName());
                    }
                }
            }
            return psiFileSet;

        } catch (IllegalAccessException e) {
            LOG.error("Error getting file list", e);
        }
        return null;
    }
    private Field findFieldInHierarchy(Class<?> startClass, String fieldName) {
        Class<?> currentClass = startClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    // 官方说这是内部API, 不建议使用
    // public static class App implements AppLifecycleListener {
    //     @Override
    //     public void appStarted() {
    //         for (Project project : ProjectManager.getInstance()
    //             .getOpenProjects()) {
    //             MessageBusConnection connection = project.getMessageBus()
    //                 .connect();
    //             connection.subscribe(ToolWindowManagerListener.TOPIC, new FindInFilesListener(project));
    //         }
    //     }
    // }

    public static class FindInFilesStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            MessageBusConnection connection = project.getMessageBus().connect();
            connection.subscribe(ToolWindowManagerListener.TOPIC, new FindInFilesListener(project));
        }
    }

}
