from fastapi import FastAPI
from python_research.routes.aqi_route import router

app = FastAPI(title="Stealth AQI API", description="API for AQI route analysis and forecasting", version="1.0.0")

app.include_router(router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)