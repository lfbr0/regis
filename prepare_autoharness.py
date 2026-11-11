#!/usr/bin/python3

import os
import re
import shutil
import json

REGIS_JAR_PATTERN = re.compile("^regis-[0-9]+\\.[0-9]+\\.[0-9]+\\.jar$")

# find regis jars in folder
def find_jars(dir: str) -> list:
    found_jars = []
    for filename in os.listdir(dir):
        if REGIS_JAR_PATTERN.match(filename):
            found_jars.append(filename)
    return found_jars

# extract version from jar name
def version(fv: str) -> str:
    return re.search(r"\d+\.\d+\.\d+", fv).group().strip()

# find baseline version (second latest version) and return abs path
def find_baseline() -> str:
    jars = find_jars("./auto-harness/jars")
    if len(jars) == 1:
        return os.path.abspath("./auto-harness/jars/" + jars[0])

    # compare function for version
    def version_key(jar: str):
        v = version(jar)
        v_tokens = v.split('.')
        return int(v_tokens[-1]) + int(v_tokens[-2])*10 + int(v_tokens[-3])*100

    # sort jars
    jars.sort(reverse=True, key=version_key)

    # return second jar
    return os.path.abspath("./auto-harness/jars/" + jars[1])

if __name__ == "__main__":
    # find latest version
    print("------------- Detecting target version -----------")
    found_versions: list = find_jars("./target")

    if len(found_versions) != 1:
        print("FOUND MORE THAN 1 TARGET JAR!")
        exit(-1)

    target_version = found_versions[0]
    print("Target JAR: ", target_version)
    print("Target Version:", version(target_version))

    # copy this jar to auto-harness folder
    print("--------------- Copying target jar ------------")
    shutil.copy("./target/" + target_version, "./auto-harness/jars")
    print("OK!")

    # find baseline and candidate
    print("--------------- Finding candidate and baseline jars ------------")
    candidate_path = os.path.abspath("./auto-harness/jars/" + target_version)
    baseline_path = find_baseline()
    print(f"Candidate: {candidate_path}; Baseline: {baseline_path}")

    # edit match.json
    print("----------------------- Editing match.json ----------------------")
    match_file = "./auto-harness/match.json"
    new_match_file_json = None

    with open(match_file) as f:
        match_conf = json.loads(f.read())
        match_conf['engines'] = [
            {"name": "Regis candidate", "command": ["java", "-jar", candidate_path]},
            {"name": "Regis baseline", "command": ["java", "-jar", baseline_path]}
        ]
        new_match_file_json = json.dumps(match_conf, indent=4)

    if new_match_file_json is None:
        print("Failed to generate new match json!")
        exit(-2)

    print("New match json: ", new_match_file_json)
    with open(match_file, "w") as f:
        f.write(new_match_file_json)

    # execute tests
    print("----------------- Executing test matches -----------------")
    print("please run in autoharness venv: " + './match.py ./match.json --out runs/candidate-vs-baseline-001')