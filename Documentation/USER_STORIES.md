# CineGrade Desktop Application: User Stories

## EPIC 1: Access Control & Onboarding

### US-01: Traditional Registration Form
**As a** New Creator  
**I want to** fill out a registration form with my email, password, and custom username  
**So that** I can create a new account via the traditional sign-up channel.

### US-02: Registration Form Validation
**As a** New Creator  
**I want to** be notified if my chosen email format is invalid, my password lacks complexity, or my username is taken  
**So that** I can correct my inputs and successfully create an account.

### US-03: Soft-Gate Auto-Login (Unverified)
**As a** New Creator  
**I want to** be logged in automatically immediately after submitting the traditional sign-up form  
**So that** I can start using local desktop features instantly without waiting for the verification email.

### US-04: Restricted Access Banner
**As an** Unverified Registered User  
**I want to** see a persistent warning banner and be blocked from community write-actions  
**So that** the platform is protected from spam while I am reminded to activate my account.

### US-05: Account Activation via Email
**As an** Unverified Registered User  
**I want to** click a single-use, time-limited activation link sent to my email  
**So that** my account transitions to VERIFIED, removing restrictions and granting full access.

### US-06: Resend Activation Email
**As an** Unverified Registered User  
**I want to** request a new activation link  
**So that** I can still verify my account if the original email was lost or expired.

### US-07: Google OAuth2 Sign-In
**As a** New Creator  
**I want to** register and log in using the "Sign in with Google" OAuth2 flow  
**So that** I can skip the traditional form and leverage my existing trusted identity.

### US-08: OAuth2 Username Auto-Generation
**As a** New Creator signing in via Google  
**I want to** have my username automatically generated based on the local part of my Google email address  
**So that** I do not have to manually type out a new username during onboarding.

### US-09: OAuth2 Username Collision Resolution
**As a** New Creator signing in via Google  
**I want to** have a deterministic suffix automatically appended to my generated username if the base name is already taken  
**So that** my account creation succeeds instantly without manual intervention.

### US-10: OAuth2 Instant Verification Access
**As a** New Creator signing in via Google  
**I want to** have my account provisioned immediately in a VERIFIED state  
**So that** I completely bypass soft-gate restrictions due to Google's identity assertion.

### US-11: Unregistered User Local Access
**As an** Unregistered User  
**I want to** have full access to local catalog management, editing, and exporting  
**So that** I can utilize the core desktop software without being forced to create an account.

### US-12: Unregistered User Web Prompt
**As an** Unregistered User  
**I want to** see a Login/Registration prompt when attempting to access the integrated web view  
**So that** I understand an account is required to interact with the community.

---

## EPIC 2: Catalog Management

### US-13: Initialize New Catalog
**As a** Photo Editor  
**I want to** initialize a new local catalog workspace  
**So that** I can start a fresh, isolated project capable of handling ~1,000 photos.

### US-14: Open Existing Catalog
**As a** Photo Editor  
**I want to** open an existing catalog from my local storage  
**So that** I can resume work on previous photography projects.

### US-15: View Recent Catalogs
**As a** Photo Editor  
**I want to** see a list of my recently opened catalogs on the start screen  
**So that** I can quickly jump back into my active projects without browsing file paths.

### US-16: Delete Catalog Permanently
**As a** Photo Editor  
**I want to** permanently delete a catalog from my local storage  
**So that** I can free up disk space when a project is completed.

### US-17: Catalog Deletion Warning
**As a** Photo Editor  
**I want to** receive a confirmation warning before a catalog is permanently deleted  
**So that** I do not accidentally lose an entire workspace and its associated edits.

---

## EPIC 3: Media Organization & File Management

### US-18: Import RAW Photos
**As a** Photo Editor  
**I want to** import new RAW photos into my active catalog  
**So that** they become available for sorting and editing.

### US-19: Drag-and-Drop Ingestion
**As a** Photo Editor  
**I want to** drag and drop RAW files from my OS directly into the File page  
**So that** I can import media quickly without navigating system dialog menus.

### US-20: Delete Photos from Catalog
**As a** Photo Editor  
**I want to** delete existing photos (individually or in bulk) from my catalog  
**So that** I can remove outtakes and irrelevant media from my workspace.

### US-20-2: Delete Photos from Physical Drive
**As a** Photo Editor  
**I want to** delete existing photos (individually or in bulk) from my Physical drive  
**So that** I can remove outtakes and irrelevant media from my OS.

### US-20-3: Catalog Deletion Warning
**As a** Photo Editor  
**I want to** receive a confirmation warning before a photo is permanently deleted
**So that** I do not accidentally lose a files.

### US-21: Create Custom Folders
**As a** Photo Editor  
**I want to** create custom folders within my catalog  
**So that** I can organize my media by scene, subject, or camera angle.

### US-22: Manage Custom Folders
**As a** Photo Editor  
**I want to** rename and delete my custom folders  
**So that** I can adjust my catalog's organizational structure as the project evolves.

### US-23: Assign Star Ratings
**As a** Photo Editor  
**I want to** assign 1-5 star ratings to my photos  
**So that** I can quickly identify my best shots for final export.

### US-24: Keyboard Shortcuts for Rating
**As a** Photo Editor  
**I want to** use my keyboard's number pad (1-5) to rate the active photo  
**So that** I can rapidly cull and score hundreds of photos without moving my mouse.

### US-25: Move Photos Between Folders
**As a** Photo Editor  
**I want to** drag and drop photos between custom folders  
**So that** I can re-categorize images easily.

### US-26: Export Physical File Hierarchy
**As a** File Manager  
**I want to** execute an organization export that copies photos to a local system drive  
**So that** my physical OS file structure perfectly mirrors the custom folder hierarchy inside the software.

---

## EPIC 4: RAW Editing & Workspace Interface

### US-27: Global Navigation
**As a** Desktop User  
**I want to** navigate seamlessly between the File and Edit pages  
**So that** I can switch between organizing my catalog and editing specific photos.

### US-28: Image Carousel Navigation
**As a** Colorist  
**I want to** navigate through my active folder using a bottom-anchored carousel via mouse or arrow keys  
**So that** I can quickly load the next RAW file into the center viewport.

### US-29: Filter Carousel View
**As a** Colorist  
**I want to** filter the carousel to display only specific folders or star ratings  
**So that** I can focus my editing session exclusively on a targeted subset of images.

### US-30: Zoom and Pan Viewport
**As a** Colorist  
**I want to** zoom in and pan around the decoded RAW image in the center viewport  
**So that** I can check focus, sharpness, and fine details while editing.

### US-31: Adjust RAW Parameters
**As a** Colorist  
**I want to** manipulate RAW parameters using sliders and color wheels in the right-side panel  
**So that** I can achieve my desired color grade and exposure corrections.

### US-32: Reset RAW Parameters
**As a** Colorist  
**I want to** click a reset button on individual sliders or the whole panel  
**So that** I can quickly revert the photo to its original unedited state if I make a mistake.

### US-33: Toggle Before/After Split Comparison
**As a** Colorist  
**I want to** drag a before/after comparison slider across the center viewport  
**So that** I can precisely evaluate my color adjustments against the unedited file.

### US-34: Copy/Paste Editing Parameters
**As a** Colorist  
**I want to** copy active editing parameters and paste them onto another photo  
**So that** I can achieve a consistent look across a sequence of similar photos.

### US-35: Save Local Preset/LUT
**As a** Colorist  
**I want to** save my current right-panel settings as a local preset or LUT  
**So that** it appears in my left-side library for future use.

### US-36: Manage Local Presets
**As a** Colorist  
**I want to** rename and delete local presets from my left-side library  
**So that** I can keep my custom color profile list organized.

### US-37: Apply Local Preset
**As a** Colorist  
**I want to** click on a saved LUT/Preset from my library  
**So that** it instantly applies the saved parameters to the active RAW image.

### US-38: Export Grade as 3D LUT
**As a** Colorist  
**I want to** export my active photo adjustment settings as a standard `.cube` file  
**So that** I can apply the same color profile in external video software like DaVinci Resolve.

---

## EPIC 5: Export Engine & Render Queue

### US-39: Configure Per-Photo Export Settings
**As a** Photo Editor  
**I want to** define specific export parameters (cropping, quality) for an individual photo in the Edit tab  
**So that** the system remembers exactly how this specific photo should be rendered.

### US-40: Configure Multiple Output Versions
**As a** Photo Editor  
**I want to** define multiple export versions of a single photo (e.g., 3 different crop/quality combos)  
**So that** I can generate tailored deliverables for different platforms simultaneously.

### US-41: Access Export Page Safely
**As a** Photo Editor  
**I want to** access the Export page exclusively from the Edit page  
**So that** the workflow logically flows from individual editing to global batch rendering.

### US-42: Configure Global Batch Settings
**As a** Photo Editor  
**I want to** select batches of photos and apply global output paths and quality settings  
**So that** I can render large groups of images efficiently to a single destination.

### US-43: Force Global Settings Overwrite
**As a** Photo Editor  
**I want to** check an option to let global batch settings overwrite individual per-photo configurations  
**So that** I can enforce a uniform render quality and crop across the entire batch if needed.

### US-44: Monitor Active Export Queue
**As a** Photo Editor  
**I want to** view a progress bar and estimated time remaining for my active export queue  
**So that** I know how long the render will take and which files are processing.

### US-45: Pause and Resume Export Queue
**As a** Photo Editor  
**I want to** pause and resume an active export queue  
**So that** I can temporarily free up CPU/GPU resources for another application without losing my batch progress.

### US-46: Cancel Export Queue
**As a** Photo Editor  
**I want to** fully stop and cancel an active export process  
**So that** I can abort the render entirely if I discover an error in my settings.

### US-47: Remove Item from Pending Queue
**As a** Photo Editor  
**I want to** remove a specific photo from the queue before it renders  
**So that** I don't waste time exporting an image I decided I no longer need.

### US-48: Open Destination on Completion
**As a** Photo Editor  
**I want to** click a prompt to open the destination folder once the queue finishes  
**So that** I can immediately view and deliver my finalized files.

---

## EPIC 6: Community & Web Integration

### US-49: Access Web View Community
**As a** Verified Registered User  
**I want to** access the integrated LUT/Preset sharing community  
**So that** I can explore color profiles shared by other creators.

### US-50: Browse and Search Community Presets
**As a** Verified Registered User  
**I want to** scroll through a feed and search for specific styles (e.g., "Cinematic", "Teal and Orange")  
**So that** I can find inspiration and new grades for my photos.

### US-51: Download Community Preset
**As a** Verified Registered User  
**I want to** download a community preset directly into my local left-side library  
**So that** I can apply other creators' grades to my own RAW files instantly.

### US-52: Publish Local LUT to Community
**As a** Verified Registered User  
**I want to** publish my locally saved LUTs to the community feed  
**So that** I can share my custom color profiles and build a reputation on the platform.

### US-53: Generate Preview with Custom Photo
**As a** Verified Registered User  
**I want to** select a specific photo from my own catalog to generate a visual preview when sharing a LUT  
**So that** the community can see a demonstration of the color grade on my personal work.

### US-54: Generate Preview with System Base Photo
**As a** Verified Registered User  
**I want to** select from three standard, system-provided base photos to generate a preview  
**So that** I can demonstrate my color grade on standard lighting scenarios without exposing my personal photos.

### US-55: Edit or Remove Published LUT
**As a** Verified Registered User  
**I want to** unpublish or edit the metadata (name, tags) of a LUT I previously shared  
**So that** I can curate my public profile and fix any naming errors.