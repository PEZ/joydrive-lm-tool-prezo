# Git History Fuzzy Search

## Project Overview

This document outlines a plan to evolve the existing Joyride-powered fuzzy search tool into a Git history search tool. The enhanced tool will allow users to search through commit history, quickly find specific commits, and view files as they existed at those points in time.

## Background

The original fuzzy search tool (`fuzzy.cljs`) provides a powerful way to search through workspace files and quickly navigate to specific lines. However, VS Code already has similar functionality built-in. By repurposing this tool to search Git history, we can create unique functionality that extends VS Code's capabilities in a valuable way.

## Core Concept

Transform the existing fuzzy search tool from searching file contents to searching Git commit history. The new tool will:

1. Display commits and their changed files in a searchable QuickPick interface
2. Allow fuzzy searching through commit messages, authors, and changed files
3. Preview files as they existed at specific commits
4. Maintain the same intuitive UI pattern established by the original tool

## Technical Approach

### Architecture

The enhanced tool will follow the same pattern as the original fuzzy search:

1. **Data Source**: Replace file system searching with Git commit history retrieval
2. **Data Transformation**: Convert commits and changed files to searchable QuickPick items
3. **User Interface**: Keep the same QuickPick UI pattern but adapted for commit data
4. **Preview Functionality**: Show files as they existed at specific commits

### Key Components

1. **Git API Integration**
   - Use VS Code's Git extension API to access repository data
   - Fetch commit history with relevant metadata
   - Retrieve file content at specific commits

2. **Data Transformation**
   - Convert commits to parent items in the QuickPick
   - Convert changed files to child items under their respective commits
   - Maintain hierarchical visualization through indentation and icons

3. **UI Components**
   - Reuse the existing QuickPick implementation
   - Adapt the preview functionality to show historical file versions
   - Enhance the display with Git-specific icons and formatting

4. **File Content Retrieval**
   - Use Git API to show file content at specific commit points
   - Create temporary documents to display historical content

## Implementation Plan

### Phase 1: REPL Exploration

1. **Explore Git API**
   - Understand available methods and data structures
   - Test commit history retrieval
   - Experiment with file content access at specific commits

2. **Prototype Key Components**
   - Create test functions for commit data transformation
   - Build prototype item creation functions
   - Test file preview functionality

3. **Build Minimal Working Example**
   - Create a simplified search interface
   - Test with a limited number of commits
   - Validate the core functionality

### Phase 2: Integration

1. **Adapt Existing Codebase**
   - Replace file search functions with Git history functions
   - Update data transformation pipeline
   - Modify the UI components as needed

2. **Enhance User Experience**
   - Add commit-specific formatting and icons
   - Implement hierarchical display for commits and files
   - Optimize performance for larger repositories

3. **Add Error Handling**
   - Handle repositories with no commits
   - Manage large repositories gracefully
   - Deal with binary files and other edge cases

### Phase 3: Refinement

1. **Performance Optimization**
   - Implement pagination for large repositories
   - Add caching for frequently accessed data
   - Optimize search algorithms

2. **Feature Enhancement**
   - Add filtering options (by author, date range, etc.)
   - Implement additional preview modes (e.g., diff view)
   - Consider integration with other Git operations

## Technical Considerations

### Performance

- Large repositories may have thousands of commits
- Each commit may contain many changed files
- Consider lazy loading or pagination for optimal performance

### User Experience

- Maintain familiar search patterns for consistency
- Use clear visual hierarchy to distinguish commits from files
- Provide helpful context in item descriptions

### Integration

- Ensure proper cleanup of temporary documents
- Handle Git extension availability gracefully
- Consider workspace-specific configurations

## Conclusion

This project represents a valuable evolution of the existing fuzzy search tool, repurposing it to provide unique functionality not readily available in VS Code. By leveraging the existing codebase and VS Code's Git extension API, we can create a powerful tool for navigating Git history with minimal development effort.

The REPL-driven development approach allows for incremental progress and experimentation, ensuring a robust and well-tested final product.
