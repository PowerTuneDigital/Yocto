# Fix unzip 6.0 failing to build on newer (C23-default) host GCC.
# See files/0001-*.patch for details.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://0001-unxcfg.h-drop-redundant-K-R-style-gmtime-localtime-.patch"
