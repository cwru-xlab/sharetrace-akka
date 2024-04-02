notebook:
	bash bin/notebook.sh

create-send-coefficient-configs:
	python bin/send-coefficient-configs.py

run:
	bash bin/run.sh $(config)

run-all:
	nohup bash bin/run-all.sh $(configs) > nohup-$(date +%s).out 2>&1 < /dev/null &