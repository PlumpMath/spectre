require_relative './proto/aspect/basic/life_state.pb'
require_relative './proto/aspect/basic/position.pb'
require_relative './proto/aspect/basic/match.pb'
require_relative './proto/aspect/basic/player.pb'
require_relative './proto/aspect/basic/times.pb'
require_relative './proto/aspect/derived/vacuum.pb'

module AspectMapper
	MAPPER = {
	    '/basic/position/hero' => {'cls' => Position::EntityPosition, 'id' => 1 },
	    '/basic/identity/player' => {'cls' => Player::PlayerIdentity, 'id' => 2 },
	    '/basic/life_state/entity' => {'cls' => LifeState::EntityLifeState, 'id' => 3 },
	    '/basic/times/time' => {'cls' => Times::GameTime, 'id' => 4 },
	    '/basic/times/state_change' => {'cls' => Times::StateChangeTime, 'id' => 5 },
	    '/basic/match/draft' => {'cls' => Match::Draft, 'id' => 6 },
	    '/derived/vacuum/group_up' => {'cls' => Vacuum::GroupUpTime, 'id' => 1000 },
	}
end