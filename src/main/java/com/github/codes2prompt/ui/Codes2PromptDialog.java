package com.github.codes2prompt.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class Codes2PromptDialog extends DialogWrapper {
    private final Project project;

    public Codes2PromptDialog(@Nullable Project project) {
        super(true);
        this.project = project;
        init();
        setTitle("codes2prompt - Prompt 内容生成");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // 这里将实现主窗口的 UI 布局
        return new JPanel();
    }
} 