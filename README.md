## Smart Attendance Management System

SmartAttendance is a desktop application built with Java Swing for managing student lists and recording attendance. It provides a simple admin login, student CRUD (add/remove), daily/monthly reporting, and CSV export — all using lightweight file-based storage for easy setup and portability.


## Overview

This app focuses on simplicity, portability, and a clear user interface — ideal for demonstrations, classroom use, or quick deployment.

## Features

- Admin login (default credentials: admin / admin123)
  
**Student management:**
- Add students (ID, Name, Year, Department)
- Remove students (auto-cleans related attendance records)

**Mark attendance:**
- Date picker to select any date
- Mark each student as Present / Absent
- Replaces previous entries for the same date to avoid duplicates

**Reports & Exports:**
- Daily report (view and export CSV)
- Monthly summary with present days, total marked, and percentage (exportable CSV)
- Export full attendance CSV

**Robust CSV handling:** custom CSV escape/parse helpers to handle commas and quotes in text

**File-based storage:** students.csv and attendance.csv (no DB required)

## Files

`SmartAttendance`

`src/SmartAttendance.java`      // Main application (UI + logic)

`students.csv`                  // Student records (auto-created)

`attendance.csv`               // Attendance records (auto-created)

`README.md`

`.gitignore`
