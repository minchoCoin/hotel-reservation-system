
/** 호텔 매니저 역할을 하는 사람들을 저장하는 테이블을 만든다.*/
create table manager(
	managerID varchar(30) primary key, --매니저 ID
	pwd	char(64) not null, -- 비밀번호(SHA256으로 암호화해서 저장)
	fname varchar(30) not null, -- 이름
	lname varchar(30) not null -- 성
);

/** 호텔을 예약하고 사용하는 손님에 대한 정보를 저장하는 테이블을 만든다.*/
create table guest(
	guestID varchar(30) primary key, -- 손님ID
	pwd char(64) not null, -- 비밀번호(SHA256으로 암호화해서 저장)
	fname varchar(30) not null, -- 이름
	lname varchar(30) not null, -- 성
	email varchar(50) not null -- 이메일
);


/** 이 호텔에 있는 방 타입에 대한 정보를 저장하는 테이블을 만든다.*/
create table rooms_type(
	name varchar(30) primary key, -- 방 타입의 이름
	single_bed_cnt int not null, -- 싱글 침대 개수
	double_bed_cnt int not null, -- 더블 침대 개수
	price int check(price>0) -- 평상시 가격
);

/** 이 호텔에 있는 방 타입의 성수기 떄의 가격을 저장하는 테이블을 만든다. */
create table special_price(
	room_type varchar(30) not null, -- 방 타입의 이름
	start_date date not null, -- 특별 가격 시작 날짜
	end_date date not null, -- 특별 가격 끝 날짜
	price int check(price>0), -- 특별 가격
	
	primary key(room_type, start_date,end_date), -- (방 타입 이름, 시작 날짜, 끝 날짜)는 유일해야한다.
	foreign key(room_type) references rooms_type(name) -- 방 타입 이름은 rooms_type의 name attribute을 참조한다.
);

/** 이 호텔에 있는 방 번호와 방 타입을 저장하는 테이블을 만든다. */
create table rooms(
	room_num int primary key, -- 방 번호
	room_type varchar(30), -- 방 타입
	foreign key(room_type) references rooms_type(name) -- 방 타입은 rooms_type 테이블의 name attribute를 참조한다.
);

/** 예약 정보를 저장하는 테이블을 만든다. */
create table reservation(
	reservationID numeric(13,0) primary key, -- 예약마다 고유로 할당되는 예약 번호이다.
	guestID varchar(30) not null, -- 예약을 한 손님의 ID이다.
	checkInDate date not null, -- 체크인 날짜
	checkOutDate date not null, -- 체크아웃 날짜
	totalCost int not null, -- 총 요금
	foreign key(guestID) references guest(guestID),
	foreign key(room_type) references rooms_type(name)
);

/** 취소된 예약 정보를 저장하는 테이블을 만든다. */
create table reserve_cancel(
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약번호이다.
	guestID varchar(30), -- 예약을 취소한 손님의 ID이다.
	cancel_date date, -- 예약을 취소한 날짜이다.
	checkInDate date not null, -- 체크인 날짜
	checkOutDate date not null, -- 체크아웃 날짜
	totalCost int not null, -- 총 요금
	primary key(reservationID, guestID),
	foreign key(guestID) references guest(guestID)
	
);

/** 각 예약이 예약한 방 타입과 개수를 저장하는 테이블을 만든다. */
create table rooms_type_reserve(
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약번호이다.
	room_type varchar(30), -- 예약한 방 타입 이름이다.
	cnt int, -- 예약한 방의 개수이다.
	primary key(reservationID, room_type),
	foreign key(reservationID) references reservation(reservationID)
);

/** 각 예약이 예약한 방 이름을 저장하는 테이블을 만든다. */
create table rooms_reserve(
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약번호이다.
	room_num int, -- 해당 예약이 예약한 방 번호
	reserve_date date, -- 해당 예약에서 이 방을 쓸 날짜
	
	primary key(room_num, reserve_date),
	foreign key(reservationID) references reservation(reservationID),
	foreign key(room_num) references rooms(room_num)
);

/** 결제 정보를 저장하는 테이블을 만든다.*/
create table bill(
	billID numeric(13,0) primary key, -- 결제마다 고유로 할당되는 결제번호이다.
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약 번호이다.
	guestID varchar(30), --결제를 진행한 손님의 ID이다.
	billDate timestamp, -- 결제한 날짜와 시간이다.
	amount int, -- 총 결제 금액이다.
	payment_method varchar(20), -- 결제 방법이다.
	
	foreign key(guestID) references guest(guestID)
);

/** 결제 취소 정보를 저장하는 테이블을 만든다. */
create table bill_cancel(
	billID numeric(13,0) primary key, -- 결제마다 고유로 할당되는 결제 번호이다.
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약 번호이다.
	guestID varchar(30), -- 결제를 취소한 손님의 ID이다.
	amount int, -- 총 결제 취소 금액이다.
	payment_method varchar(20), -- 결제 방법이다.
	cancelDate timestamp, -- 결제를 취소한 날짜와 시간이다.
	foreign key(guestID) references guest(guestID)
	
);

/** 체크인과 관련한 정보를 저장하는 테이블을 만든다.*/
create table check_in(
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약 번호이다.
	room_num int, -- 체크인하는 방 번호이다.
	guestID varchar(30), -- 체크인하는 손님의 ID이다.
	check_in_date date, -- 체크인 날짜이다.
	primary key(reservationID, room_num),
	foreign key(reservationID) references reservation(reservationID),
	foreign key(room_num) references rooms(room_num),
	foreign key(guestID) references guest(guestID)
	
);

/** 체크아웃과 관련된 정보를 저장하는 테이블을 만든다. */
create table check_out(
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약 번호이다.
	room_num int, -- 체크아웃하는 방 번호이다.
	guestID varchar(30), -- 체크아웃하는 손님의 ID이다.
	check_out_date date, -- 체크아웃 날짜이다.
	primary key(reservationID, room_num),
	foreign key(reservationID) references reservation(reservationID),
	foreign key(room_num) references rooms(room_num),
	foreign key(guestID) references guest(guestID)
	
);

/** 방 할당 내역을 저장하는 테이블을 만든다.*/
create table room_service(
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약 번호이다.
	room_num int, -- 할당된 방 번호이다.
	guestID varchar(30), -- 이 방을 쓰는 손님의 ID이다.
	service_date date, -- 이 방을 사용하는 날짜이다.
	primary key(room_num,service_date),
	foreign key(reservationID) references reservation(reservationID),
	foreign key(room_num) references rooms(room_num),
	foreign key(guestID) references guest(guestID)
	
);

/** 방을 관리하는 housekeeper의 정보를 저장하는 테이블을 만든다.*/
create table housekeeper(
	housekeeperID varchar(30), --각 housekeeper마다 고유로 할당되는 ID이다.
	fname varchar(30), -- 이름
	lname varchar(30), -- 성
	primary key(housekeeperID)
);

/** housekeep 할당 내역을 저장하는 테이블을 만든다. */
create table housekeep_assignment(
	room_num int, -- 청소할 방 번호
	housekeeperID varchar(30), -- 담당 housekeeper의 ID 
	assign_date date, -- 청소 날짜
	status varchar(11) check(status in('complete','incomplete')), -- 완료 여부. incomplete는 완료되지 않음을 의미하고, complete는 완료됨을 의미한다.
	foreign key(room_num) references rooms(room_num),
	foreign key(housekeeperID) references housekeeper(housekeeperID)
);

/** 체크인 후 발생하는 추가요금을 저장하는 테이블을 만든다.*/
create table additional_fee(
	reservationID numeric(13,0), -- 예약마다 고유로 할당되는 예약번호이다.
	guestID varchar(30), -- 추가요금을 지불할 guest의 ID이다.
	fee int, -- 추가 요금이다.
	foreign key(reservationID) references reservation(reservationID),
	foreign key(guestID) references guest(guestID)
);

/** id : manager, password : manager 의 호텔 매니저 계정을 만든다. */
insert into manager values('manager','6ee4a469cd4e91053847f5d3fcb61dbcc91e8f0ef10be7748da4c4a1ba382d17','taehun','kim');

insert into rooms_type values('double room',0,1,50000);
insert into rooms_type values('twin room',2,0,55000);
insert into rooms_type values('family room',1,1,70000);
insert into rooms_type values('double double room',0,2,75000);


insert into special_price values('double room','2023-12-20','2024-01-02',70000);
insert into special_price values('twin room','2023-12-20','2024-01-02',75000);
insert into special_price values('family room','2023-12-20','2024-01-02',90000);
insert into special_price values('double double room','2023-12-20','2024-01-02',105000);

insert into rooms values(101,'double room');
insert into rooms values(102,'double room');
insert into rooms values(103,'double room');
insert into rooms values(201,'twin room');
insert into rooms values(202,'twin room');
insert into rooms values(203,'twin room');
insert into rooms values(301,'family room');
insert into rooms values(302,'family room');
insert into rooms values(303,'family room');
insert into rooms values(401,'double double room');
insert into rooms values(402,'double double room');
insert into rooms values(403,'double double room');

insert into housekeeper values('keeper1','John','Doe');
insert into housekeeper values('keeper2','Gildong','Hong');
insert into housekeeper values('keeper3','Chung','Sim');

/*
delete from additional_fee ;
delete from bill;
delete from bill_cancel;
delete from check_in;
delete from check_out;
delete from housekeep_assignment ;
delete from room_service ;
delete from rooms_reserve ;
delete from rooms_type_reserve ;
delete from reservation;
delete from reserve_cancel ;
*/