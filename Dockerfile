# Use Ubuntu LTS as the basis for the Docker image.

FROM python:3.6-buster

# Set timezone:
RUN ln -snf /usr/share/zoneinfo/$CONTAINER_TIMEZONE /etc/localtime && echo $CONTAINER_TIMEZONE > /etc/timezone

# Install all the Linux packages required for Yocto / Toradex BSP builds. Note that the packages python3,
# tar, locales and cpio are not listed in the official Yocto / Toradex BSP documentation. The build, however, fails
# without them. curl is used for brining in the repo tool. repo tool uses git, so thats being instaled here aswell.

RUN apt-get update \
    && apt-get -y install \
    gawk wget git-core diffstat unzip texinfo gcc-multilib \
    build-essential chrpath socat cpio python python3-pexpect \
    xz-utils debianutils iputils-ping python3-git python3-jinja2 \
    libegl1-mesa libsdl1.2-dev  pylint3 xterm tar locales curl git sudo

# By default, Ubuntu uses dash as an alias for sh. Dash does not support the source command
# needed for setting up the build environment in CMD. Use bash as an alias for sh.
RUN rm /bin/sh && ln -s bash /bin/sh

# Set the locale to en_US.UTF-8, because the Yocto build fails without any locale set.
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && locale-gen
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8



# The running container writes all the build artifacts to a host directory (outside the container).
# The container can only write files to host directories, if it uses the same user ID and
# group ID owning the host directories. The host_uid and group_uid are passed to the docker build
# command with the --build-arg option. By default, they are both 1001. The docker image creates
# a group with host_gid and a user with host_uid and adds the user to the group. The symbolic
# name of the group and user is ot3.
ARG host_uid=1001
ARG host_gid=1001
ARG username=opentrons-ci
ENV USER_NAME ${username}
ENV PROJECT ot3
RUN groupadd -g $host_gid $USER_NAME || true \
    && useradd -g $host_gid -m -s /bin/bash -u $host_uid $USER_NAME || true \
    && usermod -aG sudo $USER_NAME \
    && echo "$USER_NAME  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# This volume should have the containing directory mounted into it. This is done because the
# containing directory may change frequently and should not be cached.
RUN mkdir -p /volumes/oe-core


# Perform the Yocto build as user ot3 (not as root).
# NOTE: The USER command does not set the environment variable HOME.

# By default, docker runs as root. However, Yocto builds should not be run as root, but as a 
# normal user. Hence, we switch to the newly created user ot3.
USER $USER_NAME

RUN git config --global user.name "Opentrons" && \
    git config --global user.email engineering@opentrons.com

# Create the directory structure for the Yocto build in the container. The lowest two directory
# levels must be the same as on the host.
ENV BUILD_INPUT_DIR /volumes/oe-core
ENV BUILD_OUTPUT_DIR ${BUILD_INPUT_DIR}/build
COPY start.sh /start.sh
RUN sudo chown $USER_NAME:$USER_NAME /start.sh && chmod ug+rwx /start.sh

WORKDIR $BUILD_INPUT_DIR

ENTRYPOINT ["/start.sh", "/volumes/oe-core"]
