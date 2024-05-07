import os
import sys
import argparse
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes
from tqdm import tqdm
import time
import subprocess
import zipfile
from colorama import Fore, Style

def clear_terminal():
    # Clear the terminal
    subprocess.call('clear', shell=True)

def decrypt_file(file_path, combined_key):
    chunk_size = 64 * 1024  # 64 KB

    # Split the combined key into two AES keys
    key1 = combined_key[:32]
    key2 = combined_key[32:]

    # Read IVs from the encrypted file
    with open(file_path, 'rb') as infile:
        iv1 = infile.read(16)
        iv2 = infile.read(16)

        # Create AES objects
        aes1 = AES.new(key1, AES.MODE_CBC, iv1)
        aes2 = AES.new(key2, AES.MODE_CBC, iv2)

        # Decrypt the file
        decrypted_file_path = os.path.splitext(file_path)[0]  # Remove '.enc' extension
        with open(decrypted_file_path, 'wb') as outfile:
            file_size = os.path.getsize(file_path)
            with tqdm(total=file_size, unit='B', unit_scale=True, desc='Decrypting', colour='green') as pbar:
                while True:
                    chunk = infile.read(chunk_size)
                    if len(chunk) == 0:
                        break

                    decrypted_chunk = aes2.decrypt(aes1.decrypt(chunk))
                    outfile.write(decrypted_chunk)
                    pbar.update(len(chunk))

def decrypt_folder(encrypted_zip_path, combined_key):
    decrypt_file(encrypted_zip_path, combined_key)

    # Decompress the encrypted zip file
    output_folder = "decrypted_output"
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    with zipfile.ZipFile(encrypted_zip_path, 'r') as zip_ref:
        zip_ref.extractall(output_folder)

def process_command(command):
    args = command.split()
    if args[0] == 'dec':
        if len(args) != 3:
            print("Usage: dec <file_path/folder_path> <combined_key>")
            return

        path = args[1]
        combined_key = bytes.fromhex(args[2])

        if not os.path.exists(path):
            print(f"{Fore.RED}Error: File/Folder {path} not found.{Style.RESET_ALL}")
            return

        if os.path.isfile(path):
            decrypt_file(path, combined_key)
            print(f"{Fore.LIGHTGREEN_EX}File decrypted successfully.{Style.RESET_ALL}")
        elif os.path.isdir(path):
            decrypt_folder(path, combined_key)
            print(f"{Fore.LIGHTGREEN_EX}Folder decrypted successfully.{Style.RESET_ALL}")
    else:
        print(f"Unknown command: {command}")

def main():
    clear_terminal()

    explanation = f"""
    {Fore.LIGHTCYAN_EX}Welcome to Dual AES 256 Decryption Tool!{Style.RESET_ALL}

    This tool decrypts files or folders encrypted using dual AES 256 encryption.

    Usage: dec <file_path/folder_path> <combined_key>

    Example: dec secret_file.txt.enc 356a192b7913b04c54574d18c28d46e6395428ab8b
             dec encrypted_folder.enc 356a192b7913b04c54574d18c28d46e6395428ab8b

    After decryption, the decrypted file/folder will be saved in the current directory.

    To exit, type 'exit'.
    """
    print(explanation)

    while True:
        command = input(">>> ")
        if command.lower() == 'exit':
            break
        process_command(command)

if __name__ == "__main__":
    main()
