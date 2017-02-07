"""
Flask Documentation:     http://flask.pocoo.org/docs/
Jinja2 Documentation:    http://jinja.pocoo.org/2/documentation/
Werkzeug Documentation:  http://werkzeug.pocoo.org/documentation/

This file creates your application.
"""
from operator import or_
from time import strftime

from flask import render_template, jsonify, Response
from sqlalchemy import true, false, func
from sqlalchemy.orm import aliased

from .models import Game, Checkpoint, User, Log
from .forms import MyForm,MyQForm
from datetime import datetime
from app import app, db
from .nocache import nocache


###
# Routing for your application.
###

@app.route('/')
def home():
    """Render website's home page."""
    return render_template('home.html',
                title='games',
                games=Game.query.order_by(Game.game_id).all())


@app.route('/about/')
def about():
    form = MyForm()
    return render_template('form.html', form=form)

@app.route('/test/game/<string:game_id>')
def game(game_id):
    g = Game.query.get(game_id)

    json = jsonify({'id': game_id,
                    'result': str(g)})
    response = Response(json.data)
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.mimetype = 'application/json'
    return response

@app.route('/game/<string:game_id>/checkpoint/<string:checkpoint_id>/participant/<string:participant_name>/answer/<string:answer>', methods=['GET', 'POST'])
@nocache
def punch_with_answer(game_id, checkpoint_id, participant_name, answer):
    l = Log(participant=participant_name, checkpoint_id=checkpoint_id, answer = answer)
    db.session.add(l)
    db.session.commit()

    json = jsonify({'status': 'done',
                    'id': checkpoint_id})
    response = Response(json.data)
    return response

@app.route('/game/<string:game_id>/checkpoint/<string:checkpoint_id>/participant/<string:participant_name>/punch')
@nocache
def punch(game_id, checkpoint_id, participant_name):
    g = Game.query.get(game_id);
    c = Checkpoint.query.get(checkpoint_id)
    if (g.is_questions and c.question):
        json = jsonify({'checkpoint': checkpoint_id,
                        'number': c.number,
                        'is_start': c.is_start,
                        'is_finish': c.is_finish,
                        'game': game_id,
                        'question': c.question,
                        'options':c.options})
    else:
        l = Log(participant = participant_name, checkpoint_id = checkpoint_id)
        db.session.add(l)
        db.session.commit()
        c = Checkpoint.query.get(checkpoint_id)
        json = jsonify({'checkpoint': checkpoint_id,
                        'number': c.number,
                        'is_start': c.is_start,
                        'is_finish': c.is_finish,
                        'game': game_id})
    response = Response(json.data)
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.mimetype = 'application/json'
    return response

@app.route('/game/<string:game_id>')
@nocache
def show_game_progress(game_id):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps)).order_by(Log.punch_time)
    g = Game.query.get(game_id)
    return render_template('progress.html',
                           title = 'progress',
                           logs = logs,
                           game = g)

@app.route('/game/<string:game_id>/checkpoints')
def show_game_checkpoints(game_id):
    return render_template('checkpoints.html',
                title='checkpoints',
                checkpoints = Checkpoint.query.filter(Checkpoint.game_id == game_id).order_by(Checkpoint.number),
                game_id = game_id)

@app.route('/game/<string:game_id>/results')
@nocache
def show_game_results(game_id):
    #taking the last start and finish for each participant
    sub_start = db.session.query(func.max(Log.punch_time).label('punch'), Log.participant.label('participant')).join(Checkpoint).filter(Checkpoint.is_start == true(), Checkpoint.game_id == game_id).group_by(Log.participant).subquery()
    sub_finish = db.session.query(func.max(Log.punch_time).label('punch'), Log.participant.label('participant')).join(Checkpoint).filter(Checkpoint.is_finish == true(), Checkpoint.game_id == game_id).group_by(Log.participant).subquery()
    start_logs = db.session.query(Log).join(sub_start, Log.punch_time == sub_start.c.punch and Log.participant == sub_start.c.participant and sub_start.is_start.true()).subquery()
    finish_logs = db.session.query(Log).join(sub_finish, Log.punch_time == sub_finish.c.punch and Log.participant == sub_finish.c.participant and sub_finish.is_finish.true()).subquery()
    results = db.session.query(start_logs.c.participant, (finish_logs.c.punch_time-start_logs.c.punch_time).label('result')).join(finish_logs, start_logs.c.participant == finish_logs.c.participant).order_by('result')
    g = Game.query.get(game_id)

    return render_template('results.html',
                           title = 'results',
                           results = results,
                           game=g)

@app.route('/game/<string:game_id>/clear')
@nocache
def clear_game(game_id):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps))
    for log in logs:
        db.session.delete(log)
    db.session.commit()
    response = Response()
    return response

@app.route('/game/<string:game_id>/participant/<string:participant>/register')
@nocache
def register(game_id, participant):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps)).filter(Log.participant == participant)
    logCounter = 0;
    for log in logs:
        logCounter = logCounter+1
    g = Game.query.get(game_id);
    if logCounter == 0:
        if g:
            u = User.query.get(g.user_id)
            json = jsonify({'game': game_id,
                            'game_name': g.name,
                            'freeorder': g.is_freeorder,
                            'time_limit': g.time_limit.strftime("%Y-%m-%dT'%H:%M:%S"),
                            'organizer':u.first_name + " " + u.last_name,
                            'phone':u.phone,
                            'error':''
                            })
        else:
            json = jsonify({'error': "unknown game"})
    else:
        u = User.query.get(g.user_id)
        json = jsonify({'game': game_id,
                        'game_name': g.name,
                        'freeorder': g.is_freeorder,
                        'time_limit': g.time_limit.strftime("%Y-%m-%dT%H:%M:%S"),
                        'organizer': u.first_name + " " + u.last_name,
                        'phone': u.phone,
                        'error': 'participant exists'
                        })

    response = Response(json.data)
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.mimetype = 'application/json'

    return response

@app.route('/game/<string:game_id>/message')
@nocache
def get_message(game_id):
    try:
        g = Game.query.get(game_id)
        json = jsonify({'game': g.game_id,
                        'message': g.message})
    except:
        json = jsonify({'game': game_id,
                        'message': ''})
    response = Response(json.data)
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.mimetype = 'application/json'
    return response


@app.route('/game/<string:game_id>/participant/<string:participant>')
@nocache
def show_participant_progress(game_id, participant):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps)).filter(Log.participant == participant).order_by(Log.participant)
    return render_template('progress.html',
                           title = 'progress',
                           logs = logs,
                           participant = participant)
###
# The functions below should be applicable to all Flask apps.
###

@app.route('/<file_name>.json')
def send_text_file(file_name):
    """Send your static text file."""
    file_dot_text = file_name + '.json'
    return app.send_static_file(file_dot_text)


@app.after_request
def add_header(response):
    """
    Add headers to both force latest IE rendering engine or Chrome Frame,
    and also to cache the rendered page for 10 minutes.
    """
    response.headers['X-UA-Compatible'] = 'IE=Edge,chrome=1'
    response.headers['Cache-Control'] = 'public, max-age=600'
    return response


@app.errorhandler(404)
def page_not_found(error):
    """Custom 404 page."""
    return render_template('404.html'), 404


if __name__ == '__main__':
    app.run(debug=True)
