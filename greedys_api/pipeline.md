[0KRunning with gitlab-runner 18.4.0~pre.246.g71914659 (71914659)[0;m
[0K  on green-4.saas-linux-small-amd64.runners-manager.gitlab.com/default ntHFEtyXQ, system ID: s_8990de21c550[0;m
section_start:1764428441:prepare_executor
[0K[0K[36;1mPreparing the "docker+machine" executor[0;m[0;m
[0KUsing Docker executor with image alpine:3.18 ...[0;m
[0KUsing effective pull policy of [always] for container alpine:3.18[0;m
[0KPulling docker image alpine:3.18 ...[0;m
[0KUsing docker image sha256:802c91d5298192c0f3a08101aeb5f9ade2992e22c9e27fa8b88eab82602550d0 for alpine:3.18 with digest alpine@sha256:de0eb0b3f2a47ba1eb89389859a9bd88b28e82f5826b6969ad604979713c2d4f ...[0;m
section_end:1764428447:prepare_executor
[0Ksection_start:1764428447:prepare_script
[0K[0K[36;1mPreparing environment[0;m[0;m
[0KUsing effective pull policy of [always] for container sha256:6f4a94ce4ba40b38204de66081ede1cebf111d74def77198e98050459a4f659a[0;m
Running on runner-nthfetyxq-project-74809695-concurrent-0 via runner-nthfetyxq-s-l-s-amd64-1764428396-20904da0...
section_end:1764428448:prepare_script
[0Ksection_start:1764428448:get_sources
[0K[0K[36;1mGetting source from Git repository[0;m[0;m
[32;1mGitaly correlation ID: cb6e0ba37d404660a866aa008be7a0dd[0;m
[32;1mFetching changes with git depth set to 20...[0;m
Initialized empty Git repository in /builds/greedysgroup/greedys_api/.git/
[32;1mCreated fresh repository.[0;m
[32;1mChecking out edf21a81 as detached HEAD (ref is main)...[0;m

[32;1mSkipping Git submodules setup[0;m
[32;1m$ git remote set-url origin "${CI_REPOSITORY_URL}" || echo 'Not a git repository; skipping'[0;m
section_end:1764428451:get_sources
[0Ksection_start:1764428451:restore_cache
[0K[0K[36;1mRestoring cache[0;m[0;m
[32;1mChecking cache for default-protected...[0;m
Downloading cache from https://storage.googleapis.com/gitlab-com-runners-cache/project/74809695/default-protected[0;m  ETag[0;m="e4564e7380c37ba77100b7d2d7af7930"
[32;1mSuccessfully extracted cache[0;m
section_end:1764428454:restore_cache
[0Ksection_start:1764428454:step_script
[0K[0K[36;1mExecuting "step_script" stage of the job script[0;m[0;m
[0KUsing effective pull policy of [always] for container alpine:3.18[0;m
[0KUsing docker image sha256:802c91d5298192c0f3a08101aeb5f9ade2992e22c9e27fa8b88eab82602550d0 for alpine:3.18 with digest alpine@sha256:de0eb0b3f2a47ba1eb89389859a9bd88b28e82f5826b6969ad604979713c2d4f ...[0;m
[32;1m$ apk add --no-cache openssh-client openssh-keygen[0;m
fetch https://dl-cdn.alpinelinux.org/alpine/v3.18/main/x86_64/APKINDEX.tar.gz
fetch https://dl-cdn.alpinelinux.org/alpine/v3.18/community/x86_64/APKINDEX.tar.gz
(1/6) Installing openssh-keygen (9.3_p2-r3)
(2/6) Installing ncurses-terminfo-base (6.4_p20230506-r0)
(3/6) Installing libncursesw (6.4_p20230506-r0)
(4/6) Installing libedit (20221030.3.1-r1)
(5/6) Installing openssh-client-common (9.3_p2-r3)
(6/6) Installing openssh-client-default (9.3_p2-r3)
Executing busybox-1.36.1-r7.trigger
OK: 12 MiB in 21 packages
[32;1m$ mkdir -p deploy[0;m
[32;1m$ cp "$ID_RSA" deploy/id_key[0;m
[32;1m$ tr -d '\r' < deploy/id_key > deploy/id_key.clean && mv deploy/id_key.clean deploy/id_key[0;m
[32;1m$ chmod 600 deploy/id_key[0;m
[32;1m$ mkdir -p ~/.ssh && ssh-keyscan -H "$SERVER_IP" >> ~/.ssh/known_hosts[0;m
# [MASKED]:22 SSH-2.0-OpenSSH_9.2p1 Debian-2+deb12u7
# [MASKED]:22 SSH-2.0-OpenSSH_9.2p1 Debian-2+deb12u7
# [MASKED]:22 SSH-2.0-OpenSSH_9.2p1 Debian-2+deb12u7
# [MASKED]:22 SSH-2.0-OpenSSH_9.2p1 Debian-2+deb12u7
# [MASKED]:22 SSH-2.0-OpenSSH_9.2p1 Debian-2+deb12u7
[32;1m$ echo "SSH Key loaded and ready"[0;m
SSH Key loaded and ready
[32;1m$ ssh -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i deploy/id_key [MASKED]@"$SERVER_IP" 'mkdir -p ~/greedys_api/'[0;m
[32;1m$ scp -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i deploy/id_key docker-compose.yml deploy.sh nginx.conf [MASKED]@"$SERVER_IP":~/greedys_api/[0;m
[32;1m$ scp -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -r -i deploy/id_key traefik/ [MASKED]@"$SERVER_IP":~/greedys_api/[0;m
[32;1m$ scp -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -r -i deploy/id_key rabbitmq/ [MASKED]@"$SERVER_IP":~/greedys_api/[0;m
[32;1m$ printf '%s' "$CI_JOB_TOKEN" | ssh -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i deploy/id_key [MASKED]@"$SERVER_IP" "docker login -u gitlab-ci-token --password-stdin $CI_REGISTRY"[0;m
Login Succeeded

WARNING! Your credentials are stored unencrypted in '/home/[MASKED]/.docker/config.json'.
Configure a credential helper to remove this warning. See
https://docs.docker.com/go/credential-store/

[32;1m$ ssh -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i deploy/id_key [MASKED]@"$SERVER_IP" 'cd ~/greedys_api/ && chmod +x ./deploy.sh && bash ./deploy.sh'[0;m
Pulling the latest Docker image...
latest: Pulling from greedysgroup/greedys_api
259db2ee6b87: Already exists
2e4cf50eeb92: Already exists
56ce5a7a0a8c: Already exists
e1089d61b200: Already exists
0f8b424aa0b9: Already exists
d557676654e5: Already exists
d82bc7a76a83: Already exists
d858cbc252ad: Already exists
1069fc2daed1: Already exists
b40161cd83fc: Already exists
3f4e2c586348: Already exists
eb8f5749650b: Already exists
b3c9f64f6f17: Already exists
cb7e81211404: Already exists
7c00e1d163b3: Already exists
48441044566d: Already exists
e932541b4a0d: Already exists
3d88d4f4911d: Already exists
639d1d819eb4: Already exists
6176065200ef: Already exists
4c3a7f09ec02: Already exists
46f5cfc15ec7: Already exists
45c91be2edf3: Already exists
f175d0f02a73: Already exists
24445b2631ba: Already exists
70876f70922f: Already exists
efcd351f9944: Already exists
7211dd2d49d1: Already exists
7fb7d7d62224: Already exists
b0185b402cf3: Already exists
30f6cab9f914: Already exists
8b49470fe9b2: Already exists
4017ddbccf1e: Already exists
e51132ad6222: Already exists
5764b8ccc733: Already exists
3673253a0068: Pulling fs layer
3673253a0068: Download complete
3673253a0068: Pull complete
Digest: sha256:3cbf9a14338795e9a6b290d889179e4bec7e6e85d6acbf06cdb9b5aa97aeeed7
Status: Downloaded newer image for registry.gitlab.com/greedysgroup/greedys_api:latest
registry.gitlab.com/greedysgroup/greedys_api:latest
Checking and creating traefik directory...
Creating acme.json if it doesn't exist...
Checking and creating Flutter app directory...
Removing the existing stack...
Error response from daemon: This node is not a swarm manager. Use "docker swarm init" or "docker swarm join" to connect this node to swarm and try again.
Stack not found, skipping removal.
Waiting for stack removal to complete...
Waiting for network cleanup...
Network cleanup complete
Cleaning up orphaned containers...
Pruning unused volumes...
Total reclaimed space: 0B
Additional safety wait...
Deploying the stack with Traefik + HTTPS + Flutter App...
Ignoring unsupported options: restart

Since --detach=false was not specified, tasks will be created in the background.
In a future release, --detach=false will become the default.
this node is not a swarm manager. Use "docker swarm init" or "docker swarm join" to connect this node to swarm and try again
section_end:1764428493:step_script
[0Ksection_start:1764428493:cleanup_file_variables
[0K[0K[36;1mCleaning up project directory and file based variables[0;m[0;m
section_end:1764428494:cleanup_file_variables
[0K[31;1mERROR: Job failed: exit code 1[0;m
