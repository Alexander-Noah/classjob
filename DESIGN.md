# Design

## Overview

Zhiyun Laboratory uses a restrained product UI for a data-driven laboratory operations platform. The interface is an authenticated admin console with a dark sidebar, light content surface, Element Plus components, dense data tables, forms, dialogs, and role-based navigation.

## Foundations

### Color

- App background: `#f4f6fa`
- Surface: `#ffffff`
- Primary: `#2563eb`
- Sidebar: `#0f172a`
- Primary text: `#0f172a`
- Secondary text: `#64748b`
- Muted text: `#94a3b8`
- Border: `#e2e8f0`
- Light border: `#edf2f7`

Use primary blue for active navigation, primary actions, focus states, and important state indicators. Keep inactive sidebar items subdued and avoid extra decorative accent colors.

### Typography

Primary font stack:

```css
Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
"Microsoft YaHei", "PingFang SC", Arial, sans-serif
```

The UI should use compact, predictable product typography. Prefer clear Chinese labels over abbreviated or vague names. Avoid display typography inside navigation, tables, forms, and dialogs.

### Spacing And Radius

- Sidebar width: `236px`, collapsed width: `72px`
- Header height: `64px`
- Page padding: `20px 24px 28px`
- Common radius: `8px`
- Dialog radius: `10px`

Keep cards and panels restrained. Avoid nested card layouts unless the inner element is a real tool surface such as a table, dialog, or repeated item.

## App Shell

The app shell consists of:

- Dark left sidebar for role-based navigation.
- Sticky white top header with breadcrumb, time, and user menu.
- Light gray content background for management pages.
- Element Plus menu, breadcrumb, dropdown, avatar, dialog, form, table, and button components.

Navigation should explain the business flow. For laboratory administrator pages, use five module groups:

1. 基础资源管理
2. 教学预约协同
3. 实验环境运维
4. 故障维修管理
5. 数据统计与智能辅助

This replaces implementation-driven grouping with an operations loop: resource setup, teaching coordination, environment operation, repair handling, and data intelligence.

## Components

### Sidebar Navigation

Use one home entry followed by grouped submenus. Menu group names should be short enough to fit the sidebar and formal enough for graduation defense materials.

Current laboratory administrator groups:

- 基础资源管理: 实验室资源, 设备资产, 耗材库存
- 教学预约协同: 课表采集, 课表数据, 预约审批
- 实验环境运维: 实验环境, 设备状态, 物联感知
- 故障维修管理: 故障报修, 通知公告
- 数据统计与智能辅助: 使用记录, 统计分析, 智能辅助

### Page Titles

Page titles can be more descriptive than menu labels. Use full names such as `教学课表数据采集` or `设备运行状态监测` in the header and browser title, while keeping sidebar labels compact.

### Forms And Dialogs

Use Element Plus defaults with project radius and spacing. Dialogs should be task-focused, with clear title, structured sections, and primary action on the right.

### Tables

Tables are the primary pattern for management pages. Prefer clear columns, filter bars, detail panels, and concise row actions.

## Interaction

- Sidebar hover and active states should remain consistent across groups.
- Collapsed sidebar must preserve icon meaning and not alter routing.
- Mobile sidebar opens as an overlay and closes after selecting a route.
- Motion should be limited to sidebar width/transform and simple hover/focus feedback.

## Accessibility

- Maintain strong contrast in sidebar and content labels.
- Preserve focus-visible states for interactive controls.
- Keep Chinese labels readable and avoid overly long menu items.
- Avoid using color alone to communicate status in tables and forms.
