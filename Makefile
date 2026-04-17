.PHONY: dev

dev:
	# 打开一个新窗口运行后端
	osascript -e 'tell application "Terminal" to do script "cd $(shell pwd)/Backend && ./mvnw spring-boot:run"'
	# 打开一个新窗口运行前端
	osascript -e 'tell application "Terminal" to do script "cd $(shell pwd)/Frontend && pnpm start"'

dev-backend:
	# 打开一个新窗口运行后端
	osascript -e 'tell application "Terminal" to do script "cd $(shell pwd)/Backend && ./mvnw spring-boot:run"'

dev-frontend:
	# 打开一个新窗口运行前端
	osascript -e 'tell application "Terminal" to do script "cd $(shell pwd)/Frontend && pnpm start"'
