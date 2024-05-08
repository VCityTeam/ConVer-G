import os
import subprocess
from datetime import datetime
import re


def main():
    time_xp = datetime.now().strftime("%Y-%m-%d-%H-%M-%S")

    for version in [3]:
        for products in [25]:
            for bd_cpu in [4]:
                for query_cpu in [4]:
                    for bd_ram in ['1024Mb']:
                        for query_ram in ['2048Mb']:
                            # Log configuration
                            experiment_folder = f"experiments/{time_xp}/v{version}-p{products}-bdr{bd_ram}-queryr{query_ram}-bdc{bd_cpu}-queryc{query_cpu}"
                            experiment_logs = f"{experiment_folder}/output.logs"
                            os.makedirs(experiment_folder, exist_ok=True)

                            with open(experiment_logs, 'w') as log_file:
                                log_file.write(f"[Experiment] Running with: version: {version}, products: {products}, bd_ram: {bd_ram}, queryr: {query_ram}, bd_cpu: {bd_cpu}, logs: {experiment_logs}\n")

                                with open('.env', 'w') as f_env:
                                    f_env.write(f"BD_RAM_LIMITATION={bd_ram}\n")
                                    f_env.write(f"BD_CPU_LIMITATION={bd_cpu}\n")
                                    f_env.write(f"QUERY_RAM_LIMITATION={query_ram}\n")
                                    f_env.write(f"QUERY_CPU_LIMITATION={query_cpu}\n")
                                os.rename('.env', '../.env')

                                log_file.write("-------------------------------------------- [BEGIN WORKFLOW] --------------------------------------------")

                                subprocess.run(['/bin/bash', './init_stack.sh', experiment_logs], stdout=log_file)
                                subprocess.run(['/bin/bash', './bsbm/download-2.sh', str(version), str(products)], stdout=log_file)
                                subprocess.run(['/bin/bash', './bsbm/transform-2.sh'], stdout=log_file)
                                subprocess.run(['/bin/bash', './bsbm/import_relational-2.sh'], stdout=log_file)
                                subprocess.run(['/bin/bash', './bsbm/import_triple-2.sh'], stdout=log_file)

                                log_file.write("--------------------------------------------- [END WORKFLOW] ---------------------------------------------")

                                subprocess.run(['./queries/query.sh', experiment_folder], stdout=log_file)

                            log_analysis(experiment_logs)


def log_analysis(experiment_logs):
    with open(experiment_logs, 'r') as f:
        for line in f:
            regex = r"\[Measure\] \((.*?)\):(.*?);"
            matches = re.finditer(regex, line, re.MULTILINE)
            for matchNum, match in enumerate(matches, start=1):
                print(f"Match {matchNum} was found at {match.start()}-{match.end()}: {match.group()}")

            regex2 = r"(\[.*\]) (.*).<\?xml version=\"1\.0\"\?><data modified=\"(.*?)\" milliseconds=\"(.*?)\"\/>"
            matches2 = re.finditer(regex2, line, re.MULTILINE)
            for matchNum, match in enumerate(matches2, start=1):
                print(f"Match {matchNum} was found at {match.start()}-{match.end()}: {match.group()}")


if __name__ == "__main__":
    main()
