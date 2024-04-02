notebook:
	bash bin/notebook.sh

create-send-coefficient-configs:
	python bin/send-coefficient-configs.py

run:
	bash bin/run.sh $(config) $$([ "$(cleanup)" = "false" ] && echo "--no-clean-up" || echo "")

run-all:
	# Ref: https://stackoverflow.com/a/51633521
	nohup bash bin/run-all.sh $(configs) > nohup-$$(date +"%s%3N").out 2>&1 < /dev/null &

run-send-coefficient-experiments:
	for network in "barabasi-albert" "gnm-random" "random-regular" "scale-free" "watts-strogatz"; do \
		make run-all configs="send-coefficient_${network}*.conf"; \
		sleep 0.1; \
	done
