LIBRARY ieee;
USE ieee.std_logic_1164.all;
use ieee.std_logic_arith.all;
use ieee.std_logic_unsigned.all;
use work.der_packet.all;

entity control_arbitr is
port(
		clk	: in std_logic;
		reset : in std_logic;		
		-- slave_1
		s0_wr				: in std_logic;		
		s0_rd				: in std_logic;		
		s0_wr_addr		: in std_logic_vector(31 downto 0);
		s0_wr_data		: in std_logic_vector(15 downto 0);
		s0_busy			: out std_logic;	
		s0_rd_data_ack	: out std_logic;	
		s0_rd_data		: out std_logic_vector(15 downto 0);
		-- slave_2
		s1_wr				: in std_logic;		
		s1_rd				: in std_logic;		
		s1_wr_addr		: in std_logic_vector(31 downto 0);
		s1_wr_data		: in std_logic_vector(15 downto 0);
		s1_busy			: out std_logic;	
		s1_rd_data_ack	: out std_logic;	
		s1_rd_data		: out std_logic_vector(15 downto 0);
		-- to sram (master)
		m_busy			: in std_logic;	
		m_rd_data_ack	: in std_logic;	
		m_rd_data		: in std_logic_vector(15 downto 0);
		m_wr				: out std_logic;		
		m_rd				: out std_logic;		
		m_wr_addr		: out std_logic_vector(31 downto 0);
		m_wr_data		: out std_logic_vector(15 downto 0)
) ;
end control_arbitr;

architecture control_arbitr_arch of control_arbitr is

type st_cont_state is (a0, 
							  a_wr,
							  a_rd);
signal st_cont : st_cont_state;

signal s_rd_reg, s_wr_reg	: std_logic_vector(1 downto 0);
signal s_wr_addr_reg			: m2x32;
signal s_wr_data_reg			: m2x16;
signal busy_flag, busy_flag_rst	: std_logic_vector(1 downto 0);
signal cnt_flag						: std_logic_vector(3 downto 0);


begin

s0_busy <= s0_wr or s0_rd or busy_flag(0);
s1_busy <= s1_wr or s1_rd or busy_flag(1);

process(clk, reset)
begin
if reset = '1' then
	s0_rd_data_ack	<= '0';
	s0_rd_data		<= (others => '0');	
	s1_rd_data_ack	<= '0';
	s1_rd_data		<= (others => '0');	
	m_wr				<= '0';
	m_rd				<= '0';
	m_wr_addr		<= (others => '0');
	m_wr_data		<= (others => '0');
	s_wr_reg 		<= (others => '0');
	s_rd_reg 		<= (others => '0');
	for ii in 1 downto 0 loop
		s_wr_addr_reg(ii) <= (others => '0');
		s_wr_data_reg(ii) <= (others => '0');
	end loop;
	busy_flag <= (others => '0');
	busy_flag_rst <= (others => '0');
	cnt_flag <= (others => '0');	
	
	st_cont <= a0;
elsif rising_edge(clk) then
	
	-- slave_0
	if (s0_wr = '1') or (s0_rd = '1') then
		busy_flag(0) <= '1';
		s_wr_reg(0) <= s0_wr;
		s_rd_reg(0) <= s0_rd;
		s_wr_addr_reg(0) <= s0_wr_addr;
		s_wr_data_reg(0) <= s0_wr_data;
	elsif busy_flag_rst(0) = '1' then
		busy_flag(0) <= '0';
		s_wr_reg(0) <= '0';
		s_rd_reg(0) <= '0';
	end if;
	
	-- slave_1
	if (s1_wr = '1') or (s1_rd = '1') then
		busy_flag(1) <= '1';
		s_wr_reg(1) <= s1_wr;
		s_rd_reg(1) <= s1_rd;
		s_wr_addr_reg(1) <= s1_wr_addr;
		s_wr_data_reg(1) <= s1_wr_data;
	elsif busy_flag_rst(1) = '1' then
		busy_flag(1) <= '0';
		s_wr_reg(1) <= '0';
		s_rd_reg(1) <= '0';
	end if;
	
	-- master	
	case st_cont is
	when a0	=> s0_rd_data_ack <= '0';
					s1_rd_data_ack <= '0';
					busy_flag_rst(0) <= '0';
					busy_flag_rst(1) <= '0';
					if busy_flag(conv_integer(cnt_flag)) = '1' then
						if m_busy = '0' then
							m_wr <= s_wr_reg(conv_integer(cnt_flag));
							m_rd <= s_rd_reg(conv_integer(cnt_flag));
							m_wr_addr <= s_wr_addr_reg(conv_integer(cnt_flag));
							m_wr_data <= s_wr_data_reg(conv_integer(cnt_flag));
							if s_rd_reg(conv_integer(cnt_flag)) = '1' then
								st_cont <= a_rd;
							else
								st_cont <= a_wr;
							end if;						
						end if;
					elsif cnt_flag = x"1" then
						cnt_flag <= (others => '0');
					else
						cnt_flag <= cnt_flag + '1';
					end if;
	when a_wr	=> m_wr <= '0';
						busy_flag_rst(conv_integer(cnt_flag)) <= '1';
						st_cont <= a0;
						if cnt_flag = x"1" then
							cnt_flag <= (others => '0');
						else
							cnt_flag <= cnt_flag + '1';
						end if;
	when a_rd	=> m_rd <= '0';
						if m_rd_data_ack = '1' then
							busy_flag_rst(conv_integer(cnt_flag)) <= '1';
							st_cont <= a0;
							if cnt_flag = x"0" then
								s0_rd_data_ack <= '1';
								s0_rd_data <= m_rd_data;
							elsif cnt_flag = x"1" then
								s1_rd_data_ack <= '1';
								s1_rd_data <= m_rd_data;
							end if;
							
							if cnt_flag = x"1" then
								cnt_flag <= (others => '0');
							else
								cnt_flag <= cnt_flag + '1';
							end if;
						end if;		
	end case;
end if;
end process;

end control_arbitr_arch;