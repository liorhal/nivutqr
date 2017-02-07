from flask_wtf import Form
from wtforms import StringField, BooleanField, RadioField
from wtforms.validators import DataRequired

class MyForm(Form):
    first_name = StringField(u'First Name', validators=[DataRequired()])
    last_name  = StringField(u'Last Name')

class MyQForm(Form):
     answers = RadioField('question')
     #, choices=[('1', 'value1'), ('2', 'value2'), ('3', 'value3')])