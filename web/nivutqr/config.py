import os
basedir = os.path.abspath(os.path.dirname(__file__))

WTF_CSRF_ENABLED = True
SECRET_KEY = 'you-will-never-guess'

SQLALCHEMY_DATABASE_URI = "postgres://zthpklnzkhhlpj:220a17ff42607f4eed9c473e22fcb920059292d18c6461c250118e6efe56eb85@ec2-184-72-246-219.compute-1.amazonaws.com:5432/d83bd4u8j9nkbm"
    #os.environ["DATABASE_URL"]


