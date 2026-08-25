# cantstopthesignal

# Building and Deploying

I strongly recommend using LUKS encryption for your entire disk, at the very least whichever disk/partition the
database is on. Use a strong password and do not back it up online or use it online. <br>

1. On your server box, run `git clone https://codeberg.org/TemetNosce/CantStopTheSignal.git` or if codeberg is down you
   can also use https://github.com/justmedusty/CantStopTheSignal.git <br>
2. Inside your build directory, you will **run the secret_setup.sh script** , or place the files that it creates
   manually yourself. Once the script is run, you can inspect the files in the secrets directory. You may change any of
   them if you would like. The admin password can be easily changed so that can be simpler so long as you change it, if
   it is very simple, upon first login. <br>
3. Ensure that the custom fields in src/main/resources/application.yaml match what you want, you can change the name, topic,
   motd, message_deletion_window_hours, but be aware that choosing invite only , signups_disabled, pgp_login_only,
   it
   will NOT be changeable through the admin panel. These can all be set dynamically by admins however that is only
   if
   these values are set to false in the config. Otherwise, they are **hard set to ON** <br>
   From the **root directory** after having all 3 secrets files ready, you will run
   > docker-compose up -d
   >
   and wait for it
   to be complete, when it is done, you can check the status with (while still in the root directory)
   > docker-compose ps
   >
   and ensure neither the postgres container nor the main server container have exited. <br>
4. If both containers show status as UP, then you can access the local service either on your server box if you have a
   browser on it, or on your local network by opening a browser and navigating to local-server-ip:8080. Here you can log
   in to the admin account with username admin password whatever is in the secrets/adminpassword.txt file. You can
   change your username or password, add a public key, once you are logged in. <br>
5. You can create a first post if you wish for when your service is up with an explanation , welcome message, what have
   you. <br>
6. From here we will download the i2po router, since i2pd is generally a bit better suited for server usage. On your
   Linux distro install i2pd, it is usually packaged with most distros, and ensure that the init service is enabled to
   run on boot <br>
7. You can either edit /etc/i2pd/tunnels.conf to add the entry or make a special file for it in /etc/i2pd/tunnels.conf.d
   and make a custom .conf file in there. We will prefer to edit the main tunnels.conf file at
   /etc/i2pd/tunnels.conf : <br>
   add

> [your-service-name]<br>
> type = http<br>
> host = 127.0.0.1<br>
> port = 8080<br>
> keys = your-service-name.dat<br>
>
   to your tunnels.conf file, and restart i2pd or reboot your server. Afterward navigate to 127.0.0.1:7070 and under the
   i2p tunnels banner, you will see under the Servers banner the b32 beside your-service-name from above. You will
   take ******.b32.i2p and paste that into your browser on a browser set up for I2P to visit your webpage remotely. You can
   share this b32 with anyone who you wish to know about your forum. More info for server tunnel setup
   at https://docs.i2pd.website/en/latest/user-guide/tunnels/.
   You could try setting up encrypted lease sets if you are running a private invite-only instance to keep it a bit more
   private. <br>

8. Your webservice is now up and running and ready to be used by the world in an anonymous fashion. The directory in
which you initially git cloned contains a freedom_for_all directory, this directory has your service database in it. 
This is what you will copy for backups. Inside the same initial root project directory is where you can run. The default 
admin account can be logged into with username 'admin' and password is whatever was in secrets/adminpassword.txt. If the password
is simple please change it in the profile settings page inside the service. You can also change the username if you do not like
'admin'. If you want maximum security you can add a PGP key to your admin accounnt profile and remove your password, all which
can be done within the profile settings area within the webservice, and do PGP login only.

> docker-compose down
>
to bring your service down for a backup or some other change you may wish to make. You can start the containers back up
again with docker-compose up -d from that directory or finding the images in the
> docker images
>
list and running

> docker start image_hash.
>

# Features

CantStopTheSignal is a total NO JAVASCRIPT server side rendered Kotlin webservice built with ktor and thymeleaf.
It supports creating posts with titles topics and contents. <br>
There is a rudimentary code detection algorithim that will detect if a post or comment contains code , and if so renders
it will all indents and spacing. This allows the forum to dicuss programming much easier. <br>
Lists of all popular topics can be viewed from the file icon from the feed fiew. <br>
Post contents and topics can also be searched with the magnifying glass icon in the main feed. <br>
Topics can be browsed via the file icon on the main feed page, it will list all topics in a paginated manner starting
with the most popular. <br>
It can be made invite only in which case you will need an admin generated UUID invite code to join otherwise you just
require
a username and password and optional public key that will go on your profile for others to see so they can import it
locally and encrypt messages to you. <br>
You can log in via your PGP key on file, and the site owner can make PGP login mandatory, entirely disallowing password
login altogether. <br>
PGP challenge based login brings a lot of extra security that password login can not bring. <br>
Private message conversations are supported with up to 15 members and a configurable group name. Message conversations
have timestamps so you can see when messages were sent, by who etc.<br>
You will receive notifications when people message you, you will receive notifications anytime someone comments on your
post, likes your post, likes your comment, and replies to your comment. <br>
You can upload a bio once signed up that users will see when they click your name in the comment section or on a post as
the author. <br>
You can independently sort through topic posts so you can filter posts by topic and then by likes dislikes comment count
new old etc. <br>
There is an admin panel that is accessible through the 'My Profile' button from the main feed , if you are an admin or
moderator. <br>
Moderators generally have a more read only view and can suspend users or remove posts and comments and admins can view
the logs of moderators, create new moderators or admins, suspend signups etc. <br>
There is an option when creating DMs to set auto-delete, if set, has all messages within said conversation wiped every X
hours as defined in the application.yaml file by the site hoster (only if there are no unread message
notifications). <br>
A user can leave a conversation, which deletes all of their messages, or remain within the conversation and delete all
of their messages from the database. <br>

# Development Note

This will be intended to be used through the I2P network and I do plan to make at least some pluggable values such as
MOTD, website name etc so others can use my work to host their own personal forum websites in a safe, secure manner. I2P
is ideal for this due to the architecture of the network protocol itself, Tor CAN be used however without TLS the final
node will see the plaintext, now will they know what it's for if it's just a random username and password? Probably not,
but if anything in there is identifiable or you use your name or something you use elsewhere along with a password you
use elsewhere, that is no-bueno. I am planning to set this up in a way that will be as accessible as possible for others
to host their own, and I am going to implement nice-to-haves such as suspending signups in the case of bombardment, or
straight up turning on invite-only mode in which UUID strings can be generated by admins or moderators to be given to
people who can then use that UUID to sign up. Giving the person choosing to use my software more control over what kind
of social site they wish to run.

# My Philosophy

A great power we have, is the ability to see beyond appearances. That ability, and sharing it with others, can pave the
way for change previously thought impossible. What was once a hopeless prison, with bars of steel, becomes a chrysalis
for meaningful change. Free, open, uncensored communication is what allows these prison walls to shift into something
more malleable. Sharing your observations, principles, and beliefs can help others to grow. The butterfly does not exit
the chrysalis by taking the front door, there is no door. It escapes the chrysalis by growing to the point where it can
be no longer be contained by it. With that moment giving birth to something the caterpillar could not have ever
imagined.

## Screenshots

![img.png](img.png)

![img_13.png](img_13.png)

![img_3.png](img_3.png)

![img_4.png](img_4.png)

![img_5.png](img_5.png)

![img_6.png](img_6.png)

![img_7.png](img_7.png)

![img_8.png](img_8.png)

![img_9.png](img_9.png)

![img_10.png](img_10.png)

![img_11.png](img_11.png)

![img_12.png](img_12.png)

![img_14.png](img_14.png)

