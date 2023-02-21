%define __prelink_undo_cmd %{nil}
# disable jar repacking
%global __os_install_post %(echo '%{__os_install_post}' | sed -e 's!/usr/lib[^[:space:]]*/brp-java-repack-jars[[:space:]].*$!!g')
# Change the brp-python-bytecompile script to use the AWIPS2 version of Python
%global __os_install_post %(echo '%{__os_install_post}' | sed -e 's/\/usr\/bin\/python/\/awips2\/python\/bin\/python/g')

#
# AWIPS II GEMPAK Spec File
#
Name: awips2-ncep-gempak
Summary: AWIPS II GEMPAK
Version: %{_component_version}
Release: %{_component_release}
Group: AWIPSII
BuildRoot: %{_build_root}
URL: N/A
License: N/A
Distribution: N/A
Vendor: %{_build_vendor}
Packager: %{_build_site}

AutoReq: no
Provides: awips2-ncep-gempak
Requires: awips2
Requires: awips2-python
Requires: awips2-java

BuildRequires: awips2-ant
BuildRequires: awips2-java

%description
AWIPS II GEMPAK Distribution - the AWIPS II GEMPAK subprocess application.

%prep
# Ensure that a "buildroot" has been specified.
if [ "%{_build_root}" = "" ]; then
   echo "ERROR: A BuildRoot has not been specified."
   echo "FATAL: Unable to Continue ... Terminating."
   exit 1
fi

if [ -d %{_build_root} ]; then
   rm -rf %{_build_root}
fi

%build
pushd . > /dev/null
# Build GEMPAK.
cd %{_baseline_workspace}/build.gempak
if [ $? -ne 0 ]; then
   exit 1
fi

/awips2/ant/bin/ant -f build.xml \
   -Declipse.dir=%{_uframe_eclipse} \
   build
if [ $? -ne 0 ]; then
   exit 1
fi
popd > /dev/null

%install
gempak_zip="GEMPAK-linux.gtk.x86_64.zip"

pushd . > /dev/null
cd %{_baseline_workspace}/build.gempak/gempak/tmp/I.GEMPAK
/usr/bin/unzip ${gempak_zip} -d %{_build_root}/awips2
if [ $? -ne 0 ]; then
   exit 1
fi

# add the license information.
license_dir="%{_baseline_workspace}/rpms/legal"

cp "${license_dir}/Master_Rights_File.pdf" \
   %{_build_root}/awips2/gempak
if [ $? -ne 0 ]; then
   exit 1
fi

%clean
rm -rf ${RPM_BUILD_ROOT}

%files
%defattr(644,awips,fxalpha,755)
%dir /awips2/gempak
/awips2/gempak/.eclipseproduct
%doc /awips2/gempak/about.html
/awips2/gempak/gempak.ini
%dir /awips2/gempak/configuration
/awips2/gempak/configuration/*
%dir /awips2/gempak/features
/awips2/gempak/features/*
%doc /awips2/gempak/*.pdf
%dir /awips2/gempak/plugins
/awips2/gempak/plugins/*

%defattr(755,awips,fxalpha,755)
/awips2/gempak/gempak
/awips2/gempak/gempak.sh
