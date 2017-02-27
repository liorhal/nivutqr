import os
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager


app = Flask(__name__)
app.config.from_object('config')
app.config['SECRET_KEY'] = 'the quick brown fox jumps over the lazy dog'

db = SQLAlchemy(app)

login_manager = LoginManager()
login_manager.init_app(app)

from .momentjs import momentjs
app.jinja_env.globals['momentjs'] = momentjs

from app import views, models