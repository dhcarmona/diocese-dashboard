# Admin Management Page Design Notes

This document captures the current design decisions from the `CelebrantManagementPage`
so future admin management pages can feel consistent.

## Overall page structure

- Use the shared `PageHeader` at the top of the page.
- Keep the page content simple and task-focused.
- Prefer a vertical flow instead of a dashboard-style grid when the page has one primary
  form and one primary list.
- Current recommended order:
  1. page header
  2. transient feedback alert
  3. create/edit form card
  4. directory/list card

## Card layout

- Use rounded Material UI cards with the same visual weight already used on the home page
  and other screens.
- Avoid decorative hero cards unless they add real value.
- Remove summary cards when the information can live naturally inside the page itself.
- Put small metadata, such as total record count, at the bottom of the list card instead
  of giving it its own standalone card.

## Form behavior

- Reuse a single form card for both create and edit states.
- Make the mode explicit in component state instead of inferring it only from selection.
- When the user selects an existing record, the form switches into edit mode.
- When the user clicks the reset/new action, the form returns to create mode.
- After creating a new record, return the form to create mode instead of leaving the new
  record selected for editing.
- In edit mode, show the delete action.
- In create mode, hide the delete action.

## Directory/list behavior

- Keep the directory in its own card below the form.
- Use a searchable list with immediate client-side filtering.
- Use the search field as the main list control.
- Put the clear-search affordance inside the search field as a trailing icon button.
- Show loading, error, empty, and no-match states inline inside the card.
- Make each row selectable and use the selected row to drive edit mode.
- Show lightweight row metadata, such as the record identifier, below the primary label.

## Feedback and notifications

- Show action feedback near the top of the page, directly under the page header.
- Use MUI `Alert` components for success and error feedback.
- Alerts should:
  - be dismissible with the built-in close button
  - auto-dismiss after 5 seconds
- Keep feedback messages short and action-specific.

## Copy and content density

- Prefer short, direct copy.
- Avoid large explanatory sections once the workflow is clear.
- Keep labels action-oriented and easy to scan.
- Use the same bilingual localization approach as the rest of the frontend.

## Interaction principles to keep for other management pages

- One main form, one main list.
- Fast transitions between create and edit.
- Minimal decorative content.
- Inline controls where possible instead of extra standalone buttons.
- Important state changes should be visible, but temporary.
- Preserve consistency over novelty across admin pages.

## Suggested pattern for future pages

For pages like churches, users, service templates, or reporter links:

- start with the same header + alert pattern
- place the primary form card first
- place the searchable directory/list card second
- keep record counts in the list footer
- use inline search clear controls
- use the same dismissible auto-expiring feedback pattern

