'''
This Api remote module is a server that handle whole connection
with the android Note App

input:
       Picture of music notes page in Post request

output:
       Tune of the music notes page in json
       Picture of the music notes page after processing
       
       @Part("type") RequestBody type,
       @Part MultipartBody.Part photo,
       @Part("song_speed") RequestBody _songSpeed,
       @Part("scale_type") RequestBody _scaleType,
       @Part("amount_scale") RequestBody _amountScale);

The module processing description:
       1. Receive picture in post request.
       2. Use uploadImage module to identify legality of the picture.
          store the picture on the server.
       3. Use image_edit center_image search_image modules for processing
          the picture and create tune of the music notes page.
       4. return the picture ot the music notes page and tune music.
'''

#import section
from flask import Flask, request, send_file, redirect
from main import main
from validImage import valid_image
from flask import jsonify
from MidiFile import MIDIFile
import json
import cv2
from utils import attach_full_file_name_path
            
#const section
SONG_PATH =    "https://www.noteappproject.info/downloadSong"
PICTURE_PATH = "https://www.noteappproject.info/downloadPic"
SONG_NAME = 'outputMIDI.mid'
PICTURE_NAME = 'outputImage.jpg'

app = Flask(__name__)

#route for download the song             
@app.route('/downloadSong')
def downloadSong():
    path = attach_full_file_name_path(SONG_NAME)
    return send_file(path)

#route for download the picture
@app.route('/downloadPic')
def downloadPicture():
    path = attach_full_file_name_path(PICTURE_NAME)
    return send_file(path)

#route for http request to upload the picture and parameters of the scale
@app.route('/upload_image', methods = [ 'POST'])
def image_upload(): 
    try:
        f = request.files['file']
    except KeyError:
        print('no args in the post request')
        return "no args in the post request"

    if(f.filename!=''):
        if request.method == 'POST':
            print('Http request Received')
            bool, error_massege = extract_value()
            if bool:
                print('song process success')
                return song_after_processing()
            print(error_massege)  
            return json.dumps({"error":error_massege})

#create json from the path route to download the song and the picture       
def song_after_processing():
    try:
        route_path_to_download = {"song" : SONG_PATH, "picture" : PICTURE_PATH }
        route_path_to_download = json.dumps(route_path_to_download)
        return route_path_to_download
    except Exception as err:
        print('Handling upload to the server error:', err)      

#extract and validate the file receive in the http request         
def extract_value():
    print('in extract value')
    statusExtractImagefromRequest = valid_image()
    if statusExtractImagefromRequest < 0: 
        print('error with image')  
        return False, 'error with image'    
    
    print(request.files['file'])
    print('song speed is= ' + request.form.get('song_speed'))
    print('scale type is= ' + request.form.get('scale_type'))    
    print('scale amount is= ' + request.form.get('amount_scale'))  
    print('after extract value')
       
    var = request.form.get('scale_type')
    #check the type of the scale
    if var == "0": 
        var = "natural"
    elif var == "1": 
        var = "flat"   
    elif var == "2":
        var = "sharp"
    else:
        print('scale value error')
        return False, 'scale value error'

    print('before send arg to main')
    #start the main process of the Algorithem
    bool, messege = main(request.form.get('song_speed'),request.form.get('scale_type'),request.form.get('amount_scale'))
    print('after send arg to main')
    return bool, messege
       
