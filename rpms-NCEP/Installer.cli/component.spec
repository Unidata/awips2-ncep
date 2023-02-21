%define _component_name           awips2-ncep-cli
%define _component_project_dir    Installer.cli
%define _component_default_prefix /awips2/fxa
#
# AWIPS II NCEP CLI Spec File
#
Name: %{_component_name}
Summary: AWIPS II CLI Installation
Version: %{_component_version}
Release: %{_component_release}
Group: AWIPSII
BuildRoot: /tmp
BuildArch: noarch
Prefix: %{_component_default_prefix}
URL: N/A
License: N/A
Distribution: N/A
Vendor: %{_build_vendor}

AutoReq: no
provides: awips2-ncep-cli
requires: awips2-python

%description
AWIPS II CLI Installation - Contains The AWIPS II CLI Component.

# Turn off the brp-python-bytecompile script
%global __os_install_post %(echo '%{__os_install_post}' | sed -e 's!/usr/lib[^[:space:]]*/brp-python-bytecompile[[:space:]].*$!!g')

%prep
# Verify That The User Has Specified A BuildRoot.
if [ "${RPM_BUILD_ROOT}" = "/tmp" ]
then
   echo "An Actual BuildRoot Must Be Specified. Use The --buildroot Parameter."
   echo "Unable To Continue ... Terminating"
   exit 1
fi

mkdir -p ${RPM_BUILD_ROOT}/awips2/fxa

%build

%install

# This Is The Workspace Project That Contains The Files That We
# Need For The CLI Component Installer.
CLI_PROJECT_DIR="gov.noaa.nws.ncep.edex.tools.cli"

# Create the bin Directory for the CLI Component
mkdir -p ${RPM_BUILD_ROOT}/awips2/fxa/bin
cp -r %{_baseline_workspace}/${CLI_PROJECT_DIR}/impl/* ${RPM_BUILD_ROOT}/awips2/fxa/bin

# Copy our profile.d scripts.
PROFILE_D_DIRECTORY="rpms-NCEP/Installer.cli/scripts/profile.d"
mkdir -p ${RPM_BUILD_ROOT}/etc/profile.d
cp %{_baseline_workspace}/${PROFILE_D_DIRECTORY}/* \
   ${RPM_BUILD_ROOT}/etc/profile.d

%pre
if [ "${1}" = "2" ]; then
   exit 0
fi

%post
if [ "${1}" = "2" ]; then
   exit 0
fi
PYTHON_INSTALL="/awips2/python"

%postun
if [ "${1}" = "1" ]; then
   exit 0
fi

%clean
rm -rf ${RPM_BUILD_ROOT}

%files
%defattr(644,awips,fxalpha,755)
%dir /awips2/fxa
%defattr(755,awips,fxalpha,755)
%dir /awips2/fxa/bin
%attr(755,awips,fxalpha) /awips2/fxa/bin/*
/etc/profile.d/awips2CLI.csh
/etc/profile.d/awips2CLI.sh
