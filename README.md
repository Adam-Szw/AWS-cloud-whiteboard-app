AWS IAM role link: https://068189226064.signin.aws.amazon.com/console

To develop the app:
- install Eclipse IDE
- open 2 instances of Eclipse IDE
- create 2 Eclipse workspaces - one for the server and another for client
- in each Eclipse workspace create an empty Java project
- clone git repository somewhere
- copy folder server/src into your server workspace project src folder and replace it
- do the same for the client workspace using client/src from the repository
- in both projects now right click on src in the project explorer (window on the left in Eclipse) and click 'refresh'
- for the server -> right click on Server.java and click 'run as' -> app
- for the client use App.java in application package

To ssh into instances, use 'hello=world.pem' key-pair located in repository as well


For whiteboard users:
- The client is an executable jar that doesnt require any setup. Simply download the file 'client.jar' in 'client'
directory on Gitlab (or clone the repository with it) and double click to run it. Important! It will not maintain same state
with other clients unless cloud servers are running. Running the client using eclipse project will show logs in the console
which notify of server connection/disconnection.
