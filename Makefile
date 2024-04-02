notebook:
	bash bin/notebook.sh

create-send-coefficient-configs:
	python bin/send-coefficient-configs.py

run:
	bash bin/run.sh $(config) $$([ "$(cleanup)" = "false" ] && echo "--no-clean-up" || echo "")

run-all:
	nohup bash bin/run-all.sh $(configs) > nohup-$$(date +%s).out 2>&1 < /dev/null &

run-send-coefficient-experiments:
	run-all configs="send-coefficient_barabasi-albert*.conf"
	run-all configs="send-coefficient_gnm-random*.conf"
	run-all configs="send-coefficient_random-regular*.conf"
	run-all configs="send-coefficient_scale-free*.conf"
	run-all configs="send-coefficient_watts-strogatz*.conf"