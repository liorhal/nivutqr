import os
from flask import Flask
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__)
app.config.from_object('config')
db = SQLAlchemy(app)

from .momentjs import momentjs
app.jinja_env.globals['momentjs'] = momentjs

from app import views, models