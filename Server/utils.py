'''
This utils module provide tools for the other modules.

The module feature:
    1. return home directory of required file.

'''
import os

'''
This function return the home directory path in the current package.
'''
def my_dir_path():
    try:
        path = os.path.dirname(__file__)
    except:
        print("cannot find directory path")
    return path

'''
This function Attach the name file and home directory
together, it is return path of file to store.
Input:
     Name file.
Output:
     path to store the file.              
'''
def attach_full_file_name_path(file_name):
    try:
        full_file_name_path = os.path.join(my_dir_path() , file_name)
    except:
        print("Bad directory file name")
    return full_file_name_path
