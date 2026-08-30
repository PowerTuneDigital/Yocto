# Fix pseudo's openat2() wrapper:
# 0001: conflicting type vs this host's glibc prototype (missing const
#       on the open_how* parameter) -- a build-time failure.
# 0002: the wrapper itself is an upstream ENOSYS stub -- this host's
#       glibc calls openat2() for plain file opens with no fallback, so
#       every file creation under pseudo failed during packaging. See
#       files/0002-*.patch for details.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://0001-openat2-wrapper-add-missing-const-to-match-glibc.patch \
    file://0002-openat2-implement-real-behavior-instead-of-ENOSYS-s.patch \
"
