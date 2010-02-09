Name:           dogtag-pki-console-ui
Version:        1.3.1
Release:        1%{?dist}
Summary:        Dogtag Certificate System - PKI Console User Interface
URL:            http://pki.fedoraproject.org/
License:        GPLv2
Group:          System Environment/Base

BuildArch:      noarch

BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

BuildRequires:  ant
BuildRequires:  java-devel >= 1:1.6.0
BuildRequires:  jpackage-utils
BuildRequires:  jss >= 4.2.6
BuildRequires:  ldapjdk

Requires:       java >= 1:1.6.0
Requires:       jss >= 4.2.6
Requires:       ldapjdk

Provides:       pki-console-ui = %{version}-%{release}

Obsoletes:      pki-console-ui < %{version}-%{release}

Source0:        http://pki.fedoraproject.org/pki/sources/%{name}/%{name}-%{version}.tar.gz

%description
Dogtag Certificate System is an enterprise software system designed
to manage enterprise Public Key Infrastructure (PKI) deployments.

The Dogtag PKI Console User Interface contains the graphical
user interface for the Dogtag PKI Console.

%prep

%setup -q

%build
ant \
    -Dproduct.ui.flavor.prefix="dogtag" \
    -Dproduct.prefix="pki" \
    -Dproduct="console-ui" \
    -Dversion="%{version}"

%install
rm -rf %{buildroot}
cd dist/binary
unzip %{name}-%{version}.zip -d %{buildroot}
cd %{buildroot}%{_javadir}
ln -s pki-console-theme-%{version}_en.jar pki-console-theme_en.jar

# supply convenience symlink(s) for backwards compatibility
mkdir -p %{buildroot}%{_javadir}/pki
cd %{buildroot}%{_javadir}/pki
ln -s ../pki-console-theme_en.jar cms-theme_en.jar

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%doc LICENSE
%{_javadir}/*

%changelog
* Mon Feb 8 2010 Matthew Harmsen <mharmsen@redhat.com> 1.3.1-1
- Bugzilla Bug #562986 -  Supply convenience symlink(s) for backwards
  compatibility (rename jar files as appropriate)

* Mon Jan 18 2010 Matthew Harmsen <mharmsen@redhat.com> 1.3.0-4
- Bugzilla Bug #553483 - New Package for Dogtag PKI: dogtag-pki-console-ui
- Fixed various licensing headers

* Wed Jan 6 2010 Matthew Harmsen <mharmsen@redhat.com> 1.3.0-3
- Bugzilla Bug #553483 - New Package for Dogtag PKI: dogtag-pki-console-ui
- Take ownership of directories

* Mon Dec 14 2009 Kevin Wright <kwright@redhat.com> 1.3.0-2
- Removed 'with exceptions' from License

* Wed Oct 14 2009 Ade Lee <alee@redhat.com> 1.3.0-1
- Bugzilla Bug #X - Packaging for Fedora Dogtag
