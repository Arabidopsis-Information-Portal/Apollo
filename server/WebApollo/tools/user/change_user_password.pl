#!/usr/bin/perl

use strict;
use warnings;

use DBI;
use Getopt::Long qw(:config no_ignore_case bundling);
use File::Basename;
use Crypt::PBKDF2;

my $USER_TABLE = "users";

my $dbh;
my $username;
my $password;
my $encrypted=0;

parse_options();
change_password();
cleanup();

sub parse_options {
	my $host = "localhost";
	my $port = 5432;
	my $dbname = $ENV{WEB_APOLLO_DB};
	my $dbusername = $ENV{WEB_APOLLO_DB_USER};
	my $dbpassword = $ENV{WEB_APOLLO_DB_PASS};
	my $help;
	GetOptions("host|H=s"		=> \$host,
		   "port|X=s"		=> \$port,
		   "dbname|D=s"		=> \$dbname,
		   "dbusername|U=s"	=> \$dbusername,
		   "dbpassword|P=s"	=> \$dbpassword,
		   "username|u=s"	=> \$username,
		   "password|p=s"	=> \$password,
		   "help|h"		=> \$help,
           "encrypted|e" => \$encrypted);
	print_usage() if $help;
	die "Database name is required\n" if !$dbname;
	die "User name for existing user required\n" if !$username;
	die "New password for existing user required\n" if !$password;
	my $connect_string = "dbi:Pg:host=$host;port=$port;dbname=$dbname";
	$dbh = DBI->connect($connect_string, $dbusername, $dbpassword);
}

sub print_usage {
	my $progname = basename($0);
	die << "END";
usage: $progname
	[-H|--host <user_database_host>]
	[-X|--port <user_database_port>]
	-D|--dbname <user_database_name>
	[-U|--dbusername <user_database_username>]
	[-P|--dbpassword <user_database_password>]
	-u|--username <username_for_user_to_be_added>
	-p|--password <password_for_user_to_be_added>
    [-e|--encrypted]
	[-h|--help]
END
}

sub change_password {
	my $user_id = get_user_id();
	if (!$user_id) {
		print "User $username does not exist\n";
		return;
	}

    my $sql="";
    if($encrypted) {
        my $pbkdf2 = Crypt::PBKDF2->new(
            hash_class => 'HMACSHA1', # this is the default
            iterations => 1000,      # so is this
            output_len => 20,        # and this
            salt_len => 4,           # and this.
            encoding => 'crypt',
        );  

        my $hash = $pbkdf2->generate($password);

	    $sql = "UPDATE $USER_TABLE " .
		  "SET password='$hash' " .
		  "WHERE user_id=$user_id";
    }
    else {
         $sql = "UPDATE $USER_TABLE " .
		  "SET password='$password' " .
		  "WHERE user_id=$user_id";

    }
	$dbh->do($sql);
}

sub get_user_id {
	my $sql = "SELECT * FROM $USER_TABLE WHERE username='$username'";
	my $results = $dbh->selectall_arrayref($sql);
	return $results ? $results->[0]->[0] : undef;
}

sub cleanup {
	$dbh->disconnect();
}
