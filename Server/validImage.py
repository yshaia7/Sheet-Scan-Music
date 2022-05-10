'''
This Api uploadImage module is handle the picture receive
in the post request.

The module features:
       1. identify that the picture is with a legal type.
          picture type allow are: JPEG, JPG, PNG, GIF.
       2. store the image on in the server.
'''

#import section
from flask import request, Flask
from werkzeug.utils import secure_filename
from utils import attach_full_file_name_path
import os, sys

#const section
ERROR_SAVE_FILE_NAME = -1
SUCCESS_FILE_NAME = 0
EXTENSIONS = "ALLOWED_IMAGE_EXTENSIONS"

app = Flask(__name__)
picture_to_process = None
app.config[EXTENSIONS] = ["JPEG", "JPG", "PNG", "GIF"]

'''
This function identify that image type legal
'''
def allowed_image(filename):
    
    #extension not found mean not an picture
    if not "." in filename:
        return False
    #get the extension of the image
    ext = filename.rsplit(".", 1)[1]

    #valid the extension of the image valid
    if ext.upper() in app.config[EXTENSIONS]:
        return True
    else:
        return False

'''
This function store the image in the server.
'''
def valid_image():
    print('into valid image')
    if request.files:
        image = request.files["file"]

        if image.filename == "":
            print("Erorr no filename")
            return ERROR_SAVE_FILE_NAME

        if allowed_image(image.filename):
            picture_to_process = secure_filename(image.filename)
            picture_to_process = image.filename
            print('pictuer sequence number = ' + picture_to_process)
            picture_to_process = "pic_from_usr" + picture_to_process[picture_to_process.index('.'):]
            
            #add path to the image 
            picture_to_process = attach_full_file_name_path(picture_to_process)
            
            #remove old picture and then save new one
            os.remove(picture_to_process)
            image.save(picture_to_process) 

            print("Image saved")
            return SUCCESS_FILE_NAME
        else:
            print("That file extension is not allowed")
            return ERROR_SAVE_FILE_NAME
    return ERROR_SAVE_FILE_NAME

