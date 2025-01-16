# Codebase2Prompt

[简体中文](README.md) | English

## Introduction

Codebase2Prompt is an IntelliJ IDEA plugin designed to help developers quickly generate code-related prompts. It intelligently organizes searched code files into structured prompts, facilitating code analysis and problem-solving through Large Language Models (LLMs).

This plugin is an IntelliJ IDEA extension inspired by  [CodebaseToPrompt](https://github.com/hello-nerdo/CodebaseToPrompt). It brings the functionality of CodebaseToPrompt directly into your IDE.

![image-20250116123539684](http://yr-pic.yunrong.cn/md/202501161235101.png)

## Features

- 🔍 Seamless integration with IDEA's "Find in Files" functionality
- 📁 Interactive file selection with directory tree view
- 🔄 Real-time formatted prompt generation
- 📊 Automatic token usage estimation
- 📋 One-click prompt copying
- 🌳 Folder structure visualization
- 🎯 Batch file selection and quick operations
- 💡 Smart tips and user-friendly interface

## Installation

1. Download the plugin package (Codebase2Prompt-{version}.zip)
2. In IntelliJ IDEA:
   - Go to `File` → `Settings` (Windows/Linux) or `IntelliJ IDEA` → `Preferences` (macOS)
   - Select `Plugins`
   - Click the ⚙️ icon and choose `Install Plugin from Disk...`
   - Select the downloaded plugin package
   - Restart IDE to complete installation

## Usage Guide

### Basic Workflow

1. Use "Find in Files" in IDEA to search for relevant code
2. Click the "Codebase2Prompt" button in the search results interface
3. Select desired files in the popup window
4. Click the copy button to get the generated prompt
5. Paste the prompt into your LLM conversation window

### Button Functions

- **Help**: View detailed usage instructions
- **Expand**: Expand all directory nodes
- **Collapse**: Collapse all directory nodes
- **Select All**: Select all files
- **Deselect All**: Cancel all selections
- **Copy**: Copy the generated prompt to clipboard

