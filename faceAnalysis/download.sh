read -p "Enter User: " name
MYDIR=`pwd`
scp $name@10.1.29.248:/tmp/faceAnalysis/face4mac-V01.0930.a.zip $MYDIR
