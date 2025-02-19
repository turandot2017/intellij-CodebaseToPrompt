package com.github.codebase2prompt.ui;

import com.github.codebase2prompt.storage.FileSelectionStorage;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.application.ApplicationManager;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.util.List;

public class PromptToolbarPanel extends JBPanel<PromptToolbarPanel> {
    private final Project project;
    private final Editor editor;
    private boolean hasSelectedFiles = false;
    private ActionToolbar toolbar;

    public PromptToolbarPanel(Project project, Editor editor) {
        super(new BorderLayout());
        this.project = project;
        this.editor = editor;
        initToolbar();
    }

    private void initToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // 左侧按钮组
        DefaultActionGroup leftGroup = new DefaultActionGroup();
        
        // 帮助按钮
        leftGroup.add(new AnAction("帮助", "查看插件使用说明", AllIcons.Actions.Help) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showHelpDialog();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        });

        // 分隔符
        leftGroup.add(Separator.getInstance());

        // 历史选择按钮
        leftGroup.add(new AnAction("历史选择", "查看历史选择记录", AllIcons.Actions.ListFiles) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showHistorySelections(e);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        });

        // 兼容性
        Icon saveIcon;
        try {
            // 尝试使用新版本的图标
            saveIcon = (Icon) AllIcons.Actions.class.getField("MenuSaveall").get(null);
        } catch (Exception e) {
            // 如果获取失败，使用旧版本的图标
            saveIcon = null;
        }
        leftGroup.add(new AnAction("保存选择", "保存当前选择的文件列表", saveIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onSaveSelection();
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(hasSelectedFiles);
            }
        });

        // 分隔符
        leftGroup.add(Separator.getInstance());

        // 展开按钮
        leftGroup.add(new AnAction("展开", "展开所有目录", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onExpandAll();
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        });

        // 折叠按钮
        leftGroup.add(new AnAction("折叠", "折叠所有目录", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onCollapseAll();
                }
            }
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        });

        // 分隔符
        leftGroup.add(Separator.getInstance());

        // 全选按钮
        leftGroup.add(new AnAction("全选", "选择所有文件", AllIcons.Actions.Selectall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onSelectAll();
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        });

        // 全不选按钮
        leftGroup.add(new AnAction("全不选", "取消选择所有文件", AllIcons.Actions.Unselectall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (callback != null) {
                    callback.onUnselectAll();
                }

            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        });

        // 添加左侧按钮组
        actionGroup.add(leftGroup);
        
        // 添加右对齐分隔符
        actionGroup.addSeparator();
        
        // 添加右侧按钮组
        DefaultActionGroup rightGroup = new DefaultActionGroup();

        // 添加 @复制 按钮
        AnAction atCopyAction = new AnAction("@文件列表复制", "复制文件名列表 (@file1 @file2...)", AllIcons.Actions.AddToDictionary) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyFileNamesToClipboard();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                // 只有在有选中文件时才启用
                e.getPresentation().setEnabled(hasSelectedFiles);
            }
        };
        rightGroup.add(atCopyAction);

        // 添加分隔符
        rightGroup.add(Separator.getInstance());

        // 原有的复制按钮
        AnAction copyAction = new AnAction("复制", "复制到剪贴板", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyToClipboard();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(editor != null && 
                    !editor.getDocument().getText().trim().isEmpty());
            }
        };
        rightGroup.add(copyAction);
        actionGroup.add(rightGroup);

        // 创建工具栏并保存引用
        toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true);
        toolbar.setTargetComponent(this);
        JPanel toolbarPanel = new JBPanel<>(new BorderLayout());
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
        toolbarPanel.setBorder(JBUI.Borders.empty(2, 2));
        
        add(toolbarPanel, BorderLayout.CENTER);
    }

    private void showHelpDialog() {
        String helpContent = 
            "Codebase2Prompt 插件使用说明\n\n" +
            "1. 在左侧目录树中选择需要包含的文件\n" +
            "2. 使用工具栏按钮进行操作：\n" +
            "   - 展开：展开所有目录\n" +
            "   - 折叠：折叠所有目录\n" +
            "   - 全选：选择所有文件\n" +
            "   - 全不选：取消选择所有文件\n" +
            "   - @复制：文件列表复制到剪贴板\n" +
            "   - 复制：将生成的 Prompt 复制到剪贴板\n" +
            "3. 右侧预览区域显示生成的 Prompt 内容\n" +
            "4. 底部显示已选择的文件数量和预计 Tokens\n" +
            "5. 选择列表管理\n\n" +
            "版本：1.0.4\n" +
            "作者：Tianhc";

        Messages.showInfoMessage(
            project,
            helpContent,
            "Codebase2Prompt 帮助"
        );
    }

    private void copyToClipboard() {
        String content = editor.getDocument().getText();
        if (content != null && !content.trim().isEmpty()) {
            StringSelection selection = new StringSelection(content);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            Messages.showInfoMessage(project, "内容已复制到剪贴板", "复制成功");
        }
    }

    private void showHistorySelections(AnActionEvent e) {
        FileSelectionStorage storage = FileSelectionStorage.getInstance(project);
        List<FileSelectionStorage.FileSelection> selections = storage.getAllSelections();
        
        if (selections.isEmpty()) {
            Messages.showInfoMessage(project, "暂无保存的选择记录", "历史选择");
            return;
        }
        Icon saveIcon;
        try {
            // 尝试使用新版本的图标
            saveIcon = (Icon) AllIcons.Actions.class.getField("Menu_saveall").get(null);
        } catch (Exception ex) {
            // 如果获取失败，使用旧版本的图标
            saveIcon = null;
        }
        DefaultActionGroup historyGroup = new DefaultActionGroup();
        for (FileSelectionStorage.FileSelection selection : selections) {
            DefaultActionGroup selectionGroup = new DefaultActionGroup(
                selection.getName(),
                true
            );
            selectionGroup.getTemplatePresentation().setDescription(selection.getDescription());
            selectionGroup.getTemplatePresentation().setIcon(AllIcons.Actions.ListFiles);

            // 加载选项

            selectionGroup.add(new AnAction("加载", "加载此选择", AllIcons.Actions.InSelection) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    if (callback != null) {
                        callback.onLoadSelection(selection);
                    }
                }
            });

            // 删除选项 - 使用 Cancel 图标替代 Delete
            selectionGroup.add(new AnAction("删除", "删除此选择", AllIcons.Actions.Cancel) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    // 显示确认对话框
                    int result = Messages.showYesNoDialog(
                        project,
                        String.format("确定要删除选择 %s 吗？", selection.getName()),
                        "删除确认",
                        "删除",
                        "取消",
                        Messages.getQuestionIcon()
                    );

                    if (result == Messages.YES) {
                        if (callback != null) {
                            callback.onDeleteSelection(selection);
                        }
                    }
                }
            });

            historyGroup.add(selectionGroup);
        }

        // 显示历史选择菜单
        ActionPopupMenu popupMenu = ActionManager.getInstance()
            .createActionPopupMenu(ActionPlaces.TOOLBAR, historyGroup);
        Component component = e.getInputEvent().getComponent();
        popupMenu.getComponent().show(component, 0, component.getHeight());
    }

    // 新增：复制文件名列表的方法
    private void copyFileNamesToClipboard() {
        if (callback == null) return;

        List<String> fileNames = callback.getSelectedFileNames();
        if (fileNames.isEmpty()) {
            Messages.showInfoMessage(project, "未选择任何文件", "复制失败");
            return;
        }

        // 构建 @file1 @file2 ... 格式的字符串
        StringBuilder content = new StringBuilder();
        for (String fileName : fileNames) {
            if (content.length() > 0) {
                content.append(" ");
            }
            content.append("@").append(fileName);
        }

        // 复制到剪贴板
        StringSelection selection = new StringSelection(content.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        Messages.showInfoMessage(project, 
            String.format("已复制 %d 个文件名到剪贴板", fileNames.size()), 
            "复制成功");
    }

    // 修改回调接口
    public interface ToolbarCallback {
        void onExpandAll();
        void onCollapseAll();
        void onSelectAll();
        void onUnselectAll();
        void onSaveSelection();
        void onLoadSelection(FileSelectionStorage.FileSelection selection);
        void onDeleteSelection(FileSelectionStorage.FileSelection selection); // 新增：删除选择的回调
        List<String> getSelectedFileNames(); // 新增：获取选中文件的文件名列表
    }

    private ToolbarCallback callback;

    public void setCallback(ToolbarCallback callback) {
        this.callback = callback;
    }

    // 修改更新方法，使用新的 API
    public void updateFileSelectionState(boolean hasSelection) {
        this.hasSelectedFiles = hasSelection;
        if (toolbar != null) {
            updateToolbarActionsReflectively(toolbar);
            // 有兼容问题, updateActionsImmediately 在 IDEA 2024版本已被弃用
            // toolbar.updateActionsImmediately();
        }
    }

    private void updateToolbarActionsReflectively(ActionToolbar toolbar) {
        try {
            Method updateMethod = ActionToolbar.class.getMethod("updateActionsImmediately");
            updateMethod.invoke(toolbar);
        } catch (Exception e) {
            // 如果反射调用失败，可以选择忽略异常或记录日志
            // 这里也可以考虑使用新的异步方法作为备选
            // toolbar.updateActionsAsync();
        }
    }
} 