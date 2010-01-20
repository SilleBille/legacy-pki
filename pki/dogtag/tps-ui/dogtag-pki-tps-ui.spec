Name:           dogtag-pki-tps-ui
Version:        1.3.0
Release:        6%{?dist}
Summary:        Dogtag Certificate System - Token Processing System User Interface
URL:            http://pki.fedoraproject.org/
License:        GPLv2 and LGPLv2
Group:          System Environment/Base

BuildArch:      noarch

BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

BuildRequires:  ant

Provides:       pki-tps-ui =  %{version}-%{release}

Obsoletes:      pki-tps-ui <  %{version}-%{release}

Source0:        http://pki.fedoraproject.org/pki/sources/%{name}/%{name}-%{version}.tar.gz
Patch0:         dogtag-pki-tps-ui-tarfileset.patch

%description
Dogtag Certificate System is an enterprise software system designed
to manage enterprise Public Key Infrastructure (PKI) deployments.

The Dogtag Token Processing System User Interface contains the graphical
user interface for the Dogtag Token Processing System.

%prep

%setup -q
%patch0 -p1

%build
ant \
    -Dproduct.ui.flavor.prefix="dogtag" \
    -Dproduct.prefix="pki" \
    -Dproduct="tps-ui" \
    -Dversion="%{version}"

%install
rm -rf %{buildroot}
cd dist/binary
unzip %{name}-%{version}.zip -d %{buildroot}

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%doc LICENSE
%{_datadir}/pki/

%changelog
* Wed Jan 20 2010 Dennis Gilmore <dennis@ausil.us> - 1.3.0-6
- add patch to deal with stricter syntax in ant 1.6.5

* Mon Jan 18 2010 Matthew Harmsen <mharmsen@redhat.com> 1.3.0-5
- Bugzilla Bug #553851 - New Package for Dogtag PKI: dogtag-pki-tps-ui
- Fixed various licensing headers

* Thu Jan 14 2010 Matthew Harmsen <mharmsen@redhat.com> 1.3.0-4
- Bugzilla Bug #553851 - New Package for Dogtag PKI: dogtag-pki-tps-ui
- Removed "Requires:  bash"

* Wed Jan 6 2010 Matthew Harmsen <mharmsen@redhat.com> 1.3.0-3
- Bugzilla Bug #553851 - New Package for Dogtag PKI: dogtag-pki-tps-ui
- Take ownership of directories

* Mon Dec 14 2009 Kevin Wright <kwright@redhat.com> 1.3.0-2
- Removed 'with exceptions' from License

* Fri Oct 16 2009 Ade Lee <alee@redhat.com> 1.3.0-1
- Bugzilla Bug #X - Fedora Packaging Changes
