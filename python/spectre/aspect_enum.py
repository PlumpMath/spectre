Basic = lambda:0
Basic.__dict__['Position'] = lambda:0
Basic.__dict__['Position'].__dict__['HERO'] = '/basic/position/hero'
Basic.__dict__['Identity'] = lambda:0
Basic.__dict__['Identity'].__dict__['PLAYER'] = '/basic/identity/player'
Basic.__dict__['LifeState'] = lambda:0
Basic.__dict__['LifeState'].__dict__['ENTITY'] = '/basic/life_state/entity'
Basic.__dict__['Timing'] = lambda:0
Basic.__dict__['Timing'].__dict__['CLOCK'] = '/basic/timing/clock'
Basic.__dict__['Timing'].__dict__['GAME_STATE'] = '/basic/timing/game_state'
Basic.__dict__['Match'] = lambda:0
Basic.__dict__['Match'].__dict__['DRAFT'] = '/basic/match/draft'

Derived = lambda:0
Derived.__dict__['Vacuum'] = lambda:0
Derived.__dict__['Vacuum'].__dict__['GROUP_UP'] = '/derived/vacuum/group_up'
