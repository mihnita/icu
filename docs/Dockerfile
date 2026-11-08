# The commands below work 100% if you replace `docker` with `podman`
#
# Run this only if you changed the Dockerfile:
#   docker build --tag jekyll -t jekyll .
# Run this when you want to edit and preview documentation:
#   docker run --rm -p 4000:4000 -p 35729:35729 -v $PWD:/root/docs -it --name jekyll jekyll
#
# On Windows replace $PWD with the full path to the `docs/` folder in the icu source tree.
#
# Once you have a prompt:
#   cd docs
#   bundle exec jekyll serve --host 0.0.0.0 --incremental --livereload --force_polling
#
# You can now browse the generated (local) site:
#   http://localhost:4000/icu/
#
# The doc sources in Docker are mapped to the current `docs` folder on the host.
# Any changes you make here (outside Docker) will trigger a re-generation of the site,
# and the preview in the browser will be updated automatically.
# It will take a few seconds, works better in Chrome, and has problems with the left TOC panel.
# But it is better than a manual re-generation + manual browser refresh.

FROM ubuntu:26.04

RUN apt-get update
RUN apt-get install -y g++ make openssl
RUN apt-get install -y ruby-full ruby-bundler

ENV PS1='$PWD> '

EXPOSE 4000/tcp
EXPOSE 35729/tcp

WORKDIR /root
COPY Gemfile /root
RUN bundle install

ENTRYPOINT [ "/bin/bash" ]
