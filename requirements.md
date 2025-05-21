# Slide System Requirements

## Core Requirements

1. **Markdown-Focused Authoring**
   - Use Markdown as the primary content format
   - Minimize HTML nesting to avoid indentation issues with Markdown
   - Keep HTML structure flat where possible

2. **Responsive Design**
   - Slides adapt to different screen widths and window configurations
   - Responsive behavior when horizontal space changes (e.g., when sidebar or chat opens)
   - Use container queries for component-level responsiveness

3. **Slide Templates/Types**
   - Implement reusable slide templates for common layouts
   - Keep HTML boilerplate to a minimum in each slide

4. **Content Management**
   - Scrollable content areas with maximum height constraints
   - Vertical scrolling for overflow content (especially code listings)

## Slide Types

1. **Title Slide**
   - Large header/title
   - Optional subtitle
   - Optional author information
   - Minimal additional content

2. **Two-Column Layout**
   - Flexible width distribution between columns (using a 12-grid system)
   - Common configurations: 50/50, 66/33, 33/66, 75/25
   - Responsive stacking on narrow screens
   - Each column can have its own title

3. **Image-Focused Slide**
   - Title and a prominently displayed image
   - Caption option
   - Image scaling with hover effect

4. **Content-Heavy Slide**
   - Title with vertical scrolling content area
   - Good for code listings, long bullet points
   - Visible scroll indicators

5. **Gallery/Icon Layout**
   - For displaying multiple images/icons
   - Flexible grid layout
   - Responsive reorganization

## Technical Requirements

1. **CSS Structure**
   - Use container queries for component-level responsiveness
   - Maintain consistent spacing and typography
   - Clean, maintainable class names

2. **HTML Structure**
   - Minimal nesting of HTML elements
   - Semantic HTML where possible
   - Avoid indentation in HTML blocks to prevent Markdown parsing issues

3. **User Experience**
   - Smooth transitions between slides
   - Consistent padding and margins
   - Readable typography at all screen sizes

## Nice-to-Have Features

1. **Interactive Elements**
   - Support for embedding interactive components
   - Toggle visibility of certain content

## Non-goals

1. Theming support (it's for free in VS Code)

## Current Pain Points to Address

1. Excessive HTML nesting in current slides
2. Inconsistent responsive behavior
3. Repeated HTML boilerplate across slides
4. Limited slide type options
5. Non-standardized column width management
