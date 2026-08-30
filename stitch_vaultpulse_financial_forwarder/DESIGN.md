---
name: VaultPulse
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#c6c5d5'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#908f9e'
  outline-variant: '#454653'
  surface-tint: '#bdc2ff'
  primary: '#bdc2ff'
  on-primary: '#131e8c'
  primary-container: '#818cf8'
  on-primary-container: '#101b8a'
  inverse-primary: '#4953bc'
  secondary: '#bcc7de'
  on-secondary: '#263143'
  secondary-container: '#3e495d'
  on-secondary-container: '#aeb9d0'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#00aa76'
  on-tertiary-container: '#003522'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e0e0ff'
  primary-fixed-dim: '#bdc2ff'
  on-primary-fixed: '#000767'
  on-primary-fixed-variant: '#2f3aa3'
  secondary-fixed: '#d8e3fb'
  secondary-fixed-dim: '#bcc7de'
  on-secondary-fixed: '#111c2d'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-mobile: 1rem
  margin-desktop: 2rem
  gutter: 1rem
  stack-sm: 0.5rem
  stack-md: 1rem
  stack-lg: 1.5rem
---

## Brand & Style
The design system for this utility application is rooted in the concepts of security, automation, and privacy. The brand personality is high-tech, reliable, and sophisticated, aiming to evoke a sense of absolute control and calm in the user. 

The aesthetic is a refined execution of **Corporate Modern** with **Material 3 (Material You)** principles. It prioritizes clarity and functional density while maintaining a "Sleek Tech" feel through deep backgrounds, subtle luminosity, and precise geometry. The UI should feel like a premium command center—uncluttered but powerful.

## Colors
The palette is built on a "Deep Slate" foundation to maximize visual comfort and battery efficiency. 

- **Primary (Indigo):** Used for active states, primary actions, and brand identification. It provides a technical, futuristic glow against dark backgrounds.
- **Secondary (Slate):** Used for surface containers and background layering to create a sense of depth without harsh contrast.
- **Success (Emerald):** A high-contrast indicator for secure states and completed automations.
- **Warning (Amber):** Used sparingly for alerts or pending security actions.
- **Neutral:** A deep navy-black used for the base background to ensure the primary and secondary colors appear vibrant.

## Typography
The system utilizes **Inter** across all levels to ensure maximum legibility and a systematic, utilitarian appearance. 

Headlines use tighter letter spacing and heavier weights to establish a strong hierarchy. Body text is optimized for readability with generous line heights. Labels use medium weights and slightly increased letter spacing for clarity in navigation and small UI elements like chips and data badges.

## Layout & Spacing
The layout follows the Material 3 fluid grid system. On mobile, it utilizes a 4-column grid, scaling to 12 columns on desktop. 

Spacing is governed by an 8dp linear scale. Consistent inner padding of `1rem` (16px) is applied to all container elements. For vertical stacks of cards, use `stack-md` to maintain a rhythm that feels organized and methodical.

## Elevation & Depth
In line with Material 3, depth is conveyed through **Tonal Layers** rather than heavy shadows.

1. **Level 0 (Background):** The deepest slate color (#0F172A).
2. **Level 1 (Cards/Surfaces):** A slightly lighter slate (#1E293B) with a subtle 1px border (#334155).
3. **Level 2 (Dialogs/Popovers):** Higher luminosity slate with a very soft, diffused ambient shadow (0px 4px 20px rgba(0,0,0, 0.4)).

Borders are essential to this design system; they provide the "high-tech" definition between surfaces that share similar dark tones.

## Shapes
The shape language is defined by modern, generous curves that soften the "technical" edge of the brand. All primary containers and cards use `rounded-lg` (16px) or `rounded-xl` (24px) to create a friendly but professional silhouette. Buttons and input fields follow the `rounded-lg` standard.

## Components

### Buttons
- **Primary:** Filled with the Primary Indigo color, using white or high-contrast slate text.
- **Secondary:** Outlined with a 1.5px border in Indigo, providing a lighter visual weight.
- **Tonal:** A subtle Slate background with Primary Indigo text for low-emphasis actions.

### Cards
Cards are the primary organizational unit. They should feature a 1px Slate border and 16px-24px rounded corners. Headers within cards should use `label-lg` for metadata.

### Inputs & Toggles
- **Text Fields:** Follow the Material 3 "Filled" style but with the secondary background color and a high-contrast bottom indicator.
- **Toggles:** Use the signature Material 3 pill shape. The track should be a dark neutral, and the thumb should glow with the Primary Indigo when active.

### Status Indicators
Use small, high-contrast circular "pulses" or chips. Emerald for "Protected/Active" and Amber for "Action Required." These should have a slight outer glow to simulate a physical LED light.

### Lists
Lists should be separated by subtle dividers or grouped into "In-set" sections with rounded card-like backgrounds to group related utility settings together.