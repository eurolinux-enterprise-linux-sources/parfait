Name:          parfait
Version:       0.5.4
Release:       2%{?dist}
Summary:       Java libraries for Performance Co-Pilot (PCP)
License:       ASL 2.0
URL:           https://github.com/performancecopilot/parfait
Source0:       https://github.com/performancecopilot/parfait/archive/%{version}/%{name}-%{version}.tar.gz

Patch1:        no-jcip-annotations.patch
Patch2:        no-jackson-version2.patch
Patch3:        no-more-objects.patch

%bcond_with metrics

BuildRequires: junit
BuildRequires: testng
BuildRequires: maven-local
%if 0%{?rhel} == 0
BuildRequires: maven-license-plugin
BuildRequires: maven-failsafe-plugin
BuildRequires: maven-source-plugin
%endif
BuildRequires: maven-assembly-plugin
BuildRequires: maven-plugin-bundle
BuildRequires: maven-jar-plugin
BuildRequires: maven-shade-plugin
BuildRequires: maven-install-plugin
BuildRequires: maven-surefire-plugin
BuildRequires: maven-surefire-provider-testng
BuildRequires: maven-surefire-provider-junit
BuildRequires: maven-dependency-plugin
BuildRequires: maven-verifier-plugin
%if 0%{?rhel} == 0
BuildRequires: mvn(net.jcip:jcip-annotations)
BuildRequires: mvn(org.apache.maven.wagon:wagon-ftp)
BuildRequires: mvn(org.aspectj:aspectjweaver)
BuildRequires: mvn(org.hsqldb:hsqldb)
BuildRequires: mvn(org.mockito:mockito-core)
BuildRequires: mvn(org.springframework:spring-jdbc)
BuildRequires: mvn(org.springframework:spring-core)
BuildRequires: mvn(org.springframework:spring-beans)
BuildRequires: mvn(org.springframework:spring-context)
BuildRequires: mvn(org.springframework:spring-test)
%endif
%if %{with metrics}
BuildRequires: mvn(com.codahale.metrics:metrics-core)
%endif
%if 0%{?rhel} != 0
BuildRequires: mvn(org.codehaus.jackson:jackson-core-asl)
BuildRequires: mvn(org.codehaus.jackson:jackson-mapper-asl)
%else
BuildRequires: mvn(com.fasterxml.jackson.core:jackson-core)
BuildRequires: mvn(com.fasterxml.jackson.core:jackson-annotations)
BuildRequires: mvn(com.fasterxml.jackson.core:jackson-databind)
%endif
BuildRequires: mvn(systems.uom:systems-unicode-java8)
BuildRequires: mvn(javax.measure:unit-api)
BuildRequires: mvn(tec.uom:uom-se)

BuildArch:     noarch

%description
Parfait is a Java performance monitoring library that exposes and
collects metrics through a variety of outputs.  It provides APIs
for extracting performance metrics from the JVM and other sources.
It interfaces to Performance Co-Pilot (PCP) using the Memory Mapped
Value (MMV) machinery for extremely lightweight instrumentation.

%package javadoc
BuildArch: noarch
Summary: Javadoc for Parfait

%description javadoc
This package contains the API documentation for Parfait.

%package -n pcp-parfait-agent
BuildArch: noarch
Summary: Parfait Java Agent for Performance Co-Pilot (PCP)

%description -n pcp-parfait-agent
This package contains the Parfait Agent for instrumenting Java
applications.  The agent can extract live performance metrics
from the JVM and other sources, in unmodified applications (via
the -java-agent java command line option).  It interfaces to
Performance Co-Pilot (PCP) using the Memory Mapped Value (MMV)
machinery for extremely lightweight instrumentation.

%package examples
BuildArch: noarch
Summary: Parfait Java demonstration programs

%description examples
Sample standalone Java programs showing use of Parfait modules
for instrumenting applications.

%prep
%setup -q
%if 0%{?rhel} != 0
%patch1 -p1
%patch2 -p1
%patch3 -p1
%endif

# Remove license plugin in main pom.xml
%pom_remove_plugin com.mycila:license-maven-plugin
%pom_remove_plugin com.mycila:license-maven-plugin parfait-agent

%pom_disable_module parfait-benchmark
%pom_disable_module parfait-cxf
%if %{without metrics}
%pom_disable_module parfait-dropwizard
%endif
%pom_disable_module parfait-jdbc
%if 0%{?rhel} != 0
%pom_disable_module parfait-jmx
%pom_disable_module parfait-spring
%endif
%pom_remove_plugin org.apache.maven.plugins:maven-failsafe-plugin
%pom_remove_plugin org.apache.maven.plugins:maven-jxr-plugin
%pom_remove_plugin org.apache.maven.plugins:maven-pmd-plugin
%pom_remove_plugin :maven-javadoc-plugin
%pom_remove_plugin :maven-site-plugin

%build
# skip tests for now, missing org.unitils:unitils-core:jar
%mvn_build -f
# re-instate not-shaded, not-with-all-dependencies agent jar
pushd parfait-agent/target
mv original-parfait-agent.jar parfait-agent.jar
popd

%install
%mvn_install
# install the parfait-agent extra bits (config, script, man page)
install -m 755 -d %{buildroot}%{_sysconfdir}/%{name}
install -m 644 parfait-agent/target/classes/jvm.json %{buildroot}%{_sysconfdir}/%{name}
install -m 755 -d %{buildroot}%{_bindir}
install -m 755 bin/%{name}.sh %{buildroot}%{_bindir}/%{name}
install -m 755 -d %{buildroot}%{_mandir}/man1
install -m 644 man/%{name}.1 %{buildroot}%{_mandir}/man1
# special install of shaded, with-all-dependencies agent jar
pushd parfait-agent/target
install -m 644 parfait-agent-jar-with-dependencies.jar \
            %{buildroot}%{_javadir}/%{name}/%{name}.jar
popd
# special install of with-all-dependencies sample jar files
for example in acme sleep counter
do
    pushd examples/${example}/target
    install -m 644 example-${example}-jar-with-dependencies.jar \
                %{buildroot}%{_javadir}/%{name}/${example}.jar
    popd
done

%files -f .mfiles
%doc README.md
%license LICENSE.md

%files javadoc -f .mfiles-javadoc
%license LICENSE.md

%files examples
%dir %{_javadir}/parfait
%{_javadir}/%{name}/acme.jar
%{_javadir}/%{name}/sleep.jar
%{_javadir}/%{name}/counter.jar
%doc README.md
%license LICENSE.md

%files -n pcp-parfait-agent
%dir %{_javadir}/%{name}
%{_javadir}/%{name}/%{name}.jar
%doc %{_mandir}/man1/%{name}.1*
%{_bindir}/%{name}
%doc README.md
%license LICENSE.md
%dir %{_sysconfdir}/%{name}
%config(noreplace)%{_sysconfdir}/%{name}/jvm.json

%changelog
* Wed Nov 01 2017 Nathan Scott <nathans@redhat.com> - 0.5.4-2
- Install a default configuration file for base JVM metrics.

* Fri Oct 06 2017 Nathan Scott <nathans@redhat.com> - 0.5.4-1
- Update to latest upstream sources.
- Support (patched) RHEL7 builds.

* Tue Sep 05 2017 Lukas Berk <lberk@redhat.com> - 0.5.3-1
- Update to upstream release

* Tue Aug 15 2017 Lukas Berk <lberk@redhat.com> - 0.5.1-4
- Incorprate feedback from fedora-review tool

* Thu Mar 23 2017 Nathan Scott <nathans@redhat.com> - 0.5.1-3
- Incorprate feedback from gil cattaneo for package review.

* Mon Mar 06 2017 Nathan Scott <nathans@redhat.com> - 0.5.1-1
- Update to latest upstream sources.

* Tue Feb 28 2017 Nathan Scott <nathans@redhat.com> - 0.5.0-2
- Resolve lintian errors - source, license, documentation.

* Fri Feb 24 2017 Nathan Scott <nathans@redhat.com> - 0.5.0-1
- Update to upstream release, dropping java8 patch.

* Thu Feb 16 2017 Nathan Scott <nathans@redhat.com> - 0.4.0-5
- Use RPM macros to ease dropwizard metrics enablement.
- Correct the dependency on systems.uom:systems-unicode-java8

* Fri Nov 25 2016 Nathan Scott <nathans@redhat.com> - 0.4.0-4
- Switch to uom-se and conditional use of dropwizard metrics.

* Fri Oct 28 2016 Nathan Scott <nathans@redhat.com> - 0.4.0-3
- Add in parfait wrapper shell script and man page.
- Rename the agent package to pcp-parfait-agent.
- Add in demo applications jars and parfait-examples package.

* Thu Oct 20 2016 Nathan Scott <nathans@redhat.com> - 0.4.0-2
- Addition of the standalone parfait-agent package.
- Add in proxy mode from upstream parfait code too.

* Wed Oct 12 2016 Nathan Scott <nathans@redhat.com> - 0.4.0-1
- Initial version.
