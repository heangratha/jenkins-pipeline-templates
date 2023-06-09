Jenkins LTS
===========

Setup Local Environment
-----------------------

1. Install require plunins from plugins.txt

    ```bash
        /bin/jenkins-plugin-cli -f plugins.txt
    ```

2. Copy jobs into jenkins ${JENKINS_HOME}/jobs/

3. Restart jenkins click build seed_job