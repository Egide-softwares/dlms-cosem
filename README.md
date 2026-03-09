# DLMS-COSEM

**DLMS-COSEM** is a high-performance implementation of the **DLMS/COSEM** protocol suite. This repository provides a unified toolset for communicating with smart meters via C++, and Python.

## Key Features

* **Dual-Mode Transport:** Native support for both **HDLC (IEC 62056-46)** and **TCP Wrapper (IEC 62056-47)**.
* **Asynchronous I/O:** Built on non-blocking communication with robust deadline timers and timeout handling.
* **Smart A-XDR Decoding:** Automatic translation of DLMS tags into language specific types, including specialized parsers for `DateTime` (OctetStrings).
* **State Management:** Automated HDLC sequence numbering N(S) and N(R) and association lifecycle management.
* **Permission Handling:** Built-in permissions handling.